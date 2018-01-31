package fr.coppernic.sample.columbofp.interactor;

import android.app.Activity;
import android.content.Context;

import com.integratedbiometrics.ibscanultimate.IBScan;
import com.integratedbiometrics.ibscanultimate.IBScanDevice;
import com.integratedbiometrics.ibscanultimate.IBScanException;
import com.integratedbiometrics.ibscanultimate.IBScanListener;

import fr.coppernic.sample.columbofp.settings.Settings;
import fr.coppernic.sdk.power.PowerManager;
import fr.coppernic.sdk.power.api.PowerListener;
import fr.coppernic.sdk.power.api.peripheral.Peripheral;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.powermgmt.PowerMgmt;
import fr.coppernic.sdk.powermgmt.PowerMgmtFactory;
import fr.coppernic.sdk.utils.core.CpcResult;
import timber.log.Timber;

/**
 * Created by michael on 26/01/18.
 */

public class FingerPrintInteractorImpl implements FingerprintInteractor, IBScanListener {
    private Context context;
    private static final String PRODUCT_NAME = "COLUMBO";
    /**
     * A handle to the single instance of the IBScan class that will be the primary interface to
     * the library, for operations like getting the number of scanners (getDeviceCount()) and
     * opening scanners (openDeviceAsync()).
     */
    private IBScan reader;
    /**
     * A handle to the open IBScanDevice (if any) that will be the interface for getting data from
     * the open scanner, including capturing the image (beginCaptureImage(), cancelCaptureImage()),
     * and the type of image being captured.
     */
    private IBScanDevice readerDevice;

    private PowerMgmt powerMgmt;

    private FingerprintInteractor.Listener listener;

    private FpDialogManager fpDialogManager;

    private final PowerListener powerListener = new PowerListener() {
        @Override
        public void onPowerUp(CpcResult.RESULT result, Peripheral peripheral) {
            if(result == CpcResult.RESULT.OK){
                Timber.d("Fp reader powered on");
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initializeIbScanClass();
                        listener.onReaderPoweredUp();
                    }
                });
            }
        }

        @Override
        public void onPowerDown(CpcResult.RESULT result, Peripheral peripheral) {

        }
    };

    public FingerPrintInteractorImpl(final Context context, final Listener listener) {
        this.listener = listener;
        this.context = context;
        PowerManager.get().registerListener(powerListener);
    }

    /**
     * Instantiates a IBScan object.
     */
    public void initializeIbScanClass() {
        reader = IBScan.getInstance(context);
        reader.setContext(context);
        reader.setScanListener(this);
        try {
            IBScan.DeviceDesc deviceDesc = reader.getDeviceDescription(0);
            Settings settings = new Settings(context);
            settings.setReaderName(deviceDesc.productName);
            settings.setFirmwareVersion(deviceDesc.fwVersion);
            settings.setSerialNumber(deviceDesc.serialNumber);
            settings.setProductionRevisionn(deviceDesc.devRevision);
        } catch (IBScanException e) {
            e.printStackTrace();
            Timber.e(e.getMessage());
            listener.onReaderReady(null);
        }
    }

    /**
     * Set The Finger Print Reader Property
     */
    private void initializeDeviceProperty(){
        try {
            Settings settings = new Settings(context);
            readerDevice.setProperty(IBScanDevice.PropertyId.CAPTURE_TIMEOUT, settings.getCaptureTimeout());
            readerDevice.setProperty(IBScanDevice.PropertyId.ENABLE_POWER_SAVE_MODE, settings.getPowerSaveMode());
        } catch (IBScanException e) {
            Timber.e("Fail to set reader property");
            e.printStackTrace();
        }
    }


    private void openInternal() {
        try {
            //Index of device that will be initialized it is always 0 has there is only on Finger Print Reader
            IBScan.DeviceDesc deviceDesc = reader.getDeviceDescription(0);
            if (!deviceDesc.productName.contains(PRODUCT_NAME)) {
                return;
            }

            if (readerDevice != null) {
                if (readerDevice.isOpened())
                    close();
                readerDevice = null;
            }

            reader.openDeviceAsync(0);
        } catch (IBScanException e) {
            Timber.d("Failed to open devices");
        }
    }

    /**
     * Closes the communication with Finger Print reader
     */
    public void close() {
        Timber.d("CLOSE");
        if (readerDevice != null) {
            try {
                readerDevice.close();
            } catch (IBScanException e) {
                Timber.e("Issue during device close(exeception: " + e + ")");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void captureFingerPrint() {
        if(reader == null){
            initializeIbScanClass();
        }
        openInternal();
    }

    @Override
    public void endCapture() {
        close();
    }

    @Override
    public void powerOn(boolean on) {
        if (on) {
             ConePeripheral.FP_IB_COLOMBO_USB.on(context);
            /*powerMgmt.setPower(PeripheralTypesCone.FingerPrintReader,
                    ManufacturersCone.IntegratedBiometrics, ModelsCone.Columbo,
                    InterfacesCone.UsbGpioPort, on);*/
        } else {
            ConePeripheral.FP_IB_COLOMBO_USB.off(context);
        }
    }

    @Override
    public void tearDown() {
        if (reader != null) {
            fpDialogManager.dismiss();
            close();
            reader.setContext(null);
            reader.setScanListener(null);
            reader = null;
        }
        powerOn(false);
        PowerMgmtFactory.get().setNotifier(null);
    }

    /********************IB listener *******************/

    @Override
    public void scanDeviceAttached(int i) {
        Timber.d("New IB scanner device attached");
    }

    @Override
    public void scanDeviceDetached(int i) {
        Timber.d("New IB scanner device detached");
    }

    @Override
    public void scanDevicePermissionGranted(int i, boolean b) {
        Timber.d("scanDevicePermissionGranted");
    }

    @Override
    public void scanDeviceCountChanged(int i) {

    }

    @Override
    public void scanDeviceInitProgress(int i, int i1) {

    }

    @Override
    public void scanDeviceOpenComplete(int i, IBScanDevice ibScanDevice, IBScanException e) {
        Timber.d("scanDeviceOpenComplete");
        readerDevice = ibScanDevice;
        initializeDeviceProperty();
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listener.onReaderReady(readerDevice);
                fpDialogManager = new FpDialogManager(context, readerDevice);
                fpDialogManager.show(listener);
            }
        });
    }
}
