/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.print.cts;

import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintAttributes.Margins;
import android.print.PrintAttributes.MediaSize;
import android.print.PrintAttributes.Resolution;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentAdapter.LayoutResultCallback;
import android.print.PrintDocumentAdapter.WriteResultCallback;
import android.print.PrintDocumentInfo;
import android.print.PrintJobInfo;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.print.cts.services.CustomPrintOptionsActivity;
import android.print.cts.services.FirstPrintService;
import android.print.cts.services.PrintServiceCallbacks;
import android.print.cts.services.PrinterDiscoverySessionCallbacks;
import android.print.cts.services.SecondPrintService;
import android.print.cts.services.StubbablePrinterDiscoverySession;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * This test verifies changes to the printer capabilities are applied correctly.
 */
public class CustomPrintOptionsTest extends BasePrintTest {
    private final static String LOG_TAG = "CustomPrintOptionsTest";
    private static final String PRINTER_NAME = "Test printer";

    // Default settings
    private final PageRange[] DEFAULT_PAGES = new PageRange[] { new PageRange(0, 0) };
    private final MediaSize DEFAULT_MEDIA_SIZE = MediaSize.ISO_A0;
    private final int DEFAULT_COLOR_MODE = PrintAttributes.COLOR_MODE_COLOR;
    private final int DEFAULT_DUPLEX_MODE = PrintAttributes.DUPLEX_MODE_LONG_EDGE;
    private final Resolution DEFAULT_RESOLUTION = new Resolution("300x300", "300x300", 300, 300);
    private final Margins DEFAULT_MARGINS = new Margins(0, 0, 0, 0);

    // All settings that are tested
    private final PageRange[][] PAGESS = { DEFAULT_PAGES, new PageRange[] { new PageRange(1, 1) },
            new PageRange[] { new PageRange(0, 2) }
    };
    private final MediaSize[] MEDIA_SIZES = { DEFAULT_MEDIA_SIZE, MediaSize.ISO_B0 };
    private final Integer[] COLOR_MODES = { DEFAULT_COLOR_MODE,
            PrintAttributes.COLOR_MODE_MONOCHROME
    };
    private final Integer[] DUPLEX_MODES = { DEFAULT_DUPLEX_MODE, PrintAttributes.DUPLEX_MODE_NONE
    };
    private final Resolution[] RESOLUTIONS = { DEFAULT_RESOLUTION,
            new Resolution("600x600", "600x600", 600, 600)
    };

    /**
     * Get the page ranges currently selected as described in the UI.
     *
     * @return Only page ranges from {@link #PAGESS} are detected correctly.
     *
     * @throws Exception If something was unexpected
     */
    private PageRange[] getPages() throws Exception {
        if (getUiDevice().hasObject(By.text("All 3"))) {
            return PAGESS[2];
        }

        UiObject pagesEditText = getUiDevice().findObject(new UiSelector().resourceId(
                "com.android.printspooler:id/page_range_edittext"));

        if (pagesEditText.getText().equals("2")) {
            return PAGESS[1];
        }

        if (pagesEditText.getText().equals("1")) {
            return PAGESS[0];
        }

        return null;
    }

    /**
     * Test that we can switch to a specific set of settings via the custom print options activity
     *
     * @param resetStateBefore Reset the state before the test case
     * @param layoutAttributes The layout attributes that are monitored
     * @param copyFromOriginal If the print job info should be copied from the original
     * @param numCopies        The copies to print
     * @param pages            The page ranges to print
     * @param mediaSize        The media size to use
     * @param isPortrait       If the mediaSize is portrait
     * @param colorMode        The color mode to use
     * @param duplexMode       The duplex mode to use
     * @param resolution       The resolution to use
     *
     * @throws Exception If anything is unexpected
     */
    private void testCase(boolean resetStateBefore, PrintAttributes[] layoutAttributes,
            final boolean copyFromOriginal, final Integer numCopies, final PageRange[] pages,
            final MediaSize mediaSize, final boolean isPortrait, final Integer colorMode,
            final Integer duplexMode, final Resolution resolution) throws Exception {
        final PrintAttributes.Builder additionalAttributesBuilder = new PrintAttributes.Builder();
        final PrintAttributes.Builder newAttributesBuilder = new PrintAttributes.Builder();

        if (resetStateBefore) {
            Log.d(LOG_TAG, "Reset");
            // Reset the UI state
            testCase(false, layoutAttributes, false, 1, DEFAULT_PAGES, DEFAULT_MEDIA_SIZE,
                    DEFAULT_MEDIA_SIZE.isPortrait(), DEFAULT_COLOR_MODE, DEFAULT_DUPLEX_MODE,
                    DEFAULT_RESOLUTION);
        }

        newAttributesBuilder.setMinMargins(DEFAULT_MARGINS);

        if (mediaSize != null) {
            if (isPortrait) {
                additionalAttributesBuilder.setMediaSize(mediaSize.asPortrait());
                newAttributesBuilder.setMediaSize(mediaSize.asPortrait());
            } else {
                additionalAttributesBuilder.setMediaSize(mediaSize.asLandscape());
                newAttributesBuilder.setMediaSize(mediaSize.asLandscape());
            }
        } else {
            newAttributesBuilder.setMediaSize(DEFAULT_MEDIA_SIZE);
        }

        if (colorMode != null) {
            additionalAttributesBuilder.setColorMode(colorMode);
            newAttributesBuilder.setColorMode(colorMode);
        } else {
            newAttributesBuilder.setColorMode(DEFAULT_COLOR_MODE);
        }

        if (duplexMode != null) {
            additionalAttributesBuilder.setDuplexMode(duplexMode);
            newAttributesBuilder.setDuplexMode(duplexMode);
        } else {
            newAttributesBuilder.setDuplexMode(DEFAULT_DUPLEX_MODE);
        }

        if (resolution != null) {
            additionalAttributesBuilder.setResolution(resolution);
            newAttributesBuilder.setResolution(resolution);
        } else {
            newAttributesBuilder.setResolution(DEFAULT_RESOLUTION);
        }

        CustomPrintOptionsActivity.setCallBack(
                new CustomPrintOptionsActivity.CustomPrintOptionsCallback() {
                    @Override
                    public PrintJobInfo executeCustomPrintOptionsActivity(
                            PrintJobInfo printJob, PrinterInfo printer) {
                        PrintJobInfo.Builder printJobBuilder;

                        if (copyFromOriginal) {
                            printJobBuilder = new PrintJobInfo.Builder(printJob);
                        } else {
                            printJobBuilder = new PrintJobInfo.Builder(null);
                        }

                        if (numCopies != null) {
                            printJobBuilder.setCopies(numCopies);
                        }

                        if (pages != null) {
                            printJobBuilder.setPages(pages);
                        }

                        if (mediaSize != null || colorMode != null || duplexMode != null
                                || resolution != null) {
                            printJobBuilder.setAttributes(additionalAttributesBuilder.build());
                        }

                        return printJobBuilder.build();
                    }
                });

        // Check that the attributes were send to the print service
        PrintAttributes newAttributes = newAttributesBuilder.build();
        Log.i(LOG_TAG, "Change to attributes: " + newAttributes + ", copies: " + numCopies +
                ", pages: " + Arrays.toString(pages) + ", copyFromOriginal: " + copyFromOriginal);

        // Apply options by executing callback above
        Log.d(LOG_TAG, "Apply changes");
        openCustomPrintOptions();

        Log.d(LOG_TAG, "Check attributes");
        synchronized (this) {
            long endTime = System.currentTimeMillis() + OPERATION_TIMEOUT_MILLIS;
            while (layoutAttributes[0] == null ||
                    !layoutAttributes[0].equals(newAttributes)) {
                wait(Math.max(1, endTime - System.currentTimeMillis()));

                if (endTime < System.currentTimeMillis()) {
                    throw new TimeoutException(
                            "Print attributes did not change to " + newAttributes + " in " +
                                    OPERATION_TIMEOUT_MILLIS + " ms. Current attributes"
                                    + layoutAttributes[0]);
                }
            }
        }

        PageRange[] newPages;

        if (pages == null) {
            newPages = new PageRange[] { new PageRange(0, 2) };
        } else {
            newPages = pages;
        }

        Log.d(LOG_TAG, "Check pages");
        PageRange[] actualPages = getPages();
        if (!Arrays.equals(newPages, actualPages)) {
            new AssertionError("Expected " + Arrays.toString(newPages) + ", actual " +
                    Arrays.toString(actualPages));
        }
    }

    /**
     * Tests that we can change the print settings via the custom print options activity.
     *
     * @throws Exception If something is unexpected.
     */
    public void testPrintSettingsChangeViaCustomPrintOptions() throws Exception {
        if (!supportsPrinting()) {
            return;
        }

        final PrintAttributes[] layoutAttributes = new PrintAttributes[1];

        final PrinterDiscoverySessionCallbacks firstSessionCallbacks =
                createMockPrinterDiscoverySessionCallbacks(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        StubbablePrinterDiscoverySession session =
                                ((PrinterDiscoverySessionCallbacks) invocation.getMock())
                                        .getSession();
                        PrinterId printerId = session.getService().generatePrinterId(PRINTER_NAME);
                        List<PrinterInfo> printers = new ArrayList<>(1);
                        PrinterCapabilitiesInfo.Builder builder =
                                new PrinterCapabilitiesInfo.Builder(printerId);

                        builder.setMinMargins(DEFAULT_MARGINS)
                                .setColorModes(COLOR_MODES[0] | COLOR_MODES[1],
                                        DEFAULT_COLOR_MODE)
                                .setDuplexModes(DUPLEX_MODES[0] | DUPLEX_MODES[1],
                                        DEFAULT_DUPLEX_MODE)
                                .addMediaSize(DEFAULT_MEDIA_SIZE, true)
                                .addMediaSize(MEDIA_SIZES[1], false)
                                .addResolution(DEFAULT_RESOLUTION, true)
                                .addResolution(RESOLUTIONS[1], false);

                        printers.add(new PrinterInfo.Builder(printerId, PRINTER_NAME,
                                PrinterInfo.STATUS_IDLE).setCapabilities(builder.build()).build());

                        session.addPrinters(printers);
                        return null;
                    }
                }, null, null, null, null, null, new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        onPrinterDiscoverySessionDestroyCalled();
                        return null;
                    }
                });

        PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
                new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) throws Throwable {
                        LayoutResultCallback callback = (LayoutResultCallback) invocation
                                .getArguments()[3];
                        PrintDocumentInfo info = new PrintDocumentInfo.Builder(PRINT_JOB_NAME)
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .setPageCount(3)
                                .build();

                        synchronized (CustomPrintOptionsTest.this) {
                            layoutAttributes[0] = (PrintAttributes) invocation.getArguments()[1];

                            CustomPrintOptionsTest.this.notifyAll();
                        }

                        callback.onLayoutFinished(info, true);
                        return null;
                    }
                },
                new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                        WriteResultCallback callback = (WriteResultCallback) args[3];

                        PageRange[] writtenPages = (PageRange[]) args[0];

                        writeBlankPages(layoutAttributes[0], fd, writtenPages[0].getStart(),
                                writtenPages[0].getEnd());
                        fd.close();

                        callback.onWriteFinished(writtenPages);
                        return null;
                    }
                }, null);

        // Create the service callbacks for the first print service.
        PrintServiceCallbacks firstServiceCallbacks = createMockPrintServiceCallbacks(
                new Answer<PrinterDiscoverySessionCallbacks>() {
                    @Override
                    public PrinterDiscoverySessionCallbacks answer(InvocationOnMock invocation) {
                        return firstSessionCallbacks;
                    }
                }, null, null);

        // Configure the print services.
        FirstPrintService.setCallbacks(firstServiceCallbacks);
        SecondPrintService.setCallbacks(createMockPrintServiceCallbacks(null, null, null));

        print(adapter);

        selectPrinter(PRINTER_NAME);
        openPrintOptions();

        // All combinations would take hours, hence run only an interesting subset

        // Change everything
        testCase(true, layoutAttributes, false, 2, PAGESS[1], MEDIA_SIZES[1], false, COLOR_MODES[1],
                DUPLEX_MODES[1], RESOLUTIONS[1]);

        // Change only attributes
        testCase(true, layoutAttributes, false, null, null, MEDIA_SIZES[1], false, COLOR_MODES[1],
                DUPLEX_MODES[1], RESOLUTIONS[1]);

        // Change only non-attributes
        testCase(true, layoutAttributes, false, 2, PAGESS[1], null, true, null, null, null);

        // Change only attributes, no copy from original
        testCase(true, layoutAttributes, true, null, null, MEDIA_SIZES[1], false, COLOR_MODES[1],
                DUPLEX_MODES[1], RESOLUTIONS[1]);

        // Change only non-attributes, no copy from original
        testCase(true, layoutAttributes, true, 2, PAGESS[1], null, true, null, null, null);

        // Change to default
        testCase(false, layoutAttributes, false, 1, DEFAULT_PAGES, DEFAULT_MEDIA_SIZE,
                DEFAULT_MEDIA_SIZE.isPortrait(), DEFAULT_COLOR_MODE, DEFAULT_DUPLEX_MODE,
                DEFAULT_RESOLUTION);

        // Change to default, no copy from original
        testCase(false, layoutAttributes, true, 1, DEFAULT_PAGES, DEFAULT_MEDIA_SIZE,
                DEFAULT_MEDIA_SIZE.isPortrait(), DEFAULT_COLOR_MODE, DEFAULT_DUPLEX_MODE,
                DEFAULT_RESOLUTION);

        // Change nothing
        testCase(true, layoutAttributes, false, null, null, null, true, null, null, null);

        // Change nothing, no copy from original
        testCase(true, layoutAttributes, true, null, null, null, true, null, null, null);

        // Change to all pages
        testCase(true, layoutAttributes, false, null, PAGESS[2], null, true, null, null, null);

        // Abort printing
        getUiDevice().pressBack();
    }
}
