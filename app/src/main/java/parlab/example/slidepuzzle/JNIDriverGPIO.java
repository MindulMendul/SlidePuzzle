package parlab.example.slidepuzzle;

import android.util.Log;

public class JNIDriverGPIO {
    private boolean mConnectFlag;

    private TranseThreadGPIO mTranseThread;
    private JNIListener mMainActivity;
    static {
        System.loadLibrary("JNIDriverGPIO");
    }

    private native static int openDriverGPIO(String path);
    private native static void closeDriverGPIO();
    private native char readDriverGPIO();
    private native int getInterruptGPIO();
    public  JNIDriverGPIO() { mConnectFlag = false; }

    public void onReceiveGPIO(int val){
        Log.e("test","4");
        if(mMainActivity!=null){
            mMainActivity.onReceiveGPIO(val);
            Log.e("test","2");
        }
    }
    public void setListenerGPIO(JNIListener a) { mMainActivity = a; }

    public int openGPIO(String driver){
        if(mConnectFlag) return -1;
        if(openDriverGPIO(driver)>0){
            mConnectFlag = true;
            mTranseThread = new TranseThreadGPIO();
            mTranseThread.start();
            return 1;
        } else
            return -1;
    }

    public void closeGPIO(){
        if(!mConnectFlag) return;
        mConnectFlag = false;
        closeDriverGPIO();
    }

    protected void finalizeGPIO() throws Throwable{
        closeGPIO();
        super.finalize();
    }

    public char readGPIO() { return readDriverGPIO(); }

    private class TranseThreadGPIO extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                while(mConnectFlag) {
                    try {Log.e("test", "1");
                        onReceiveGPIO(getInterruptGPIO());
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }catch (Exception e){}
        }
    }
}