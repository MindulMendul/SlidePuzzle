package parlab.example.slidepuzzle;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.hardware.Camera.PictureCallback;
import android.view.View;


public class MainActivity extends AppCompatActivity implements JNIListener {
    //common variable
    int difficulty=-1;
    int movement=0;
    int[][] puzzleMask;
    int[] witch={0, 0};

    //GPIO
    String str="";

    JNIDriverGPIO mDriverGPIO;

    //7SEG
    int data_int, i;
    boolean mThreadRun, mStart;
    SegmentThread mSegThread;

    //LED
    byte[] data = {0,0,0,0,0,0,0,0};

    //CAM
    private static final String TAG = "CamTestActivity";
    private Camera mCamera;
    private CameraPreview mPreview;
    private ImageView capturedImageHolder;
    private ImageView preview2;

    //OpenCL
    private Bitmap original_bitmap;
    private Bitmap buf_bitmap;

    static {
        System.loadLibrary("JNIDriver7seg");
        System.loadLibrary("JNIDriverLED");
        System.loadLibrary("OpenCLDriver");
    }

    private native static int openDriver7seg(String path);
    private native static void closeDriver7seg();
    private native static void writeDriver7seg(byte[] data, int length);
    private native static int openDriverLED(String path);
    private native static void closeDriverLED();
    private native static void writeDriverLED(byte[] data, int length);
    private native Bitmap puzzleGPU(Bitmap bitmap, int[][] puzzleMask);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //GIPO
        mDriverGPIO = new JNIDriverGPIO();
        mDriverGPIO.setListenerGPIO(this);

        if(mDriverGPIO.openGPIO("/dev/sm9s5422_interrupt")<0){
            Toast.makeText(MainActivity.this, "Driver Open Failed GPIO", Toast.LENGTH_SHORT).show();
        }

        //7SEG
        Button btn = (Button) findViewById(R.id.buttonCapture);

        //CAM
        capturedImageHolder = (ImageView) findViewById(R.id.captured_image);

        mCamera=getCameraInstance();
        mCamera.setDisplayOrientation(180);
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview2=(ImageView) findViewById(R.id.captured_image2);

        preview.addView(mPreview);

        btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //TODO Auto-generated method stub
                String str = ((EditText)findViewById(R.id.editNUM)).getText().toString();
                if(!str.equals("")) {
                    if(Integer.parseInt(str)<2 || Integer.parseInt(str)>8){
                        Toast.makeText(MainActivity.this, "(1<x<9)(1<x<9)(1<x<9)(1<x<9)(1<x<9)", Toast.LENGTH_SHORT).show();
                    }else {
                        try {
                            movement = 0;
                            data_int = Integer.parseInt(str);
                            mStart = true;
                            for (i = 0; i < 8; i++) {
                                if (data_int > i) data[i] = 1;
                                else data[i] = 0;
                            }
                            difficulty = data_int;
                            puzzleMask = new int[difficulty][difficulty];
                            puzzleFunc.initArray(puzzleMask, difficulty);
                            puzzleMask[difficulty - 1][difficulty - 1] = 0;

                            puzzleFunc.randomArray(puzzleMask, witch, difficulty);
                            while (!puzzleFunc.isSolvable(puzzleMask, difficulty, witch[1])) {
                                puzzleFunc.randomArray(puzzleMask, witch, difficulty);
                            }
                            puzzleFunc.logArray(puzzleMask, difficulty);

                            Toast.makeText(MainActivity.this, "Slide : " + difficulty + " X " + difficulty, Toast.LENGTH_SHORT).show();
                            writeDriverLED(data, data.length);
                        } catch (NumberFormatException E) {
                            Toast.makeText(MainActivity.this, "Input Error", Toast.LENGTH_SHORT).show();
                        }
                        mSegThread = new SegmentThread();
                        mSegThread.start();

                        mCamera.takePicture(null, null, pictureCallback);
                        preview.removeView(mPreview);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Ip-Ryeok must be required", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //GPIO
    protected void onPauseGPIO(){
        mDriverGPIO.closeGPIO();
        super.onPause();
    }

    public Handler handler = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){
            if(difficulty>0) {
                switch (msg.arg1) {
                    case 1:
                        movement++;
                        puzzleFunc.moveUp(puzzleMask, witch, difficulty);
                        break;
                    case 2:
                        movement++;
                        puzzleFunc.moveDown(puzzleMask, witch, difficulty);
                        break;
                    case 3:
                        movement++;
                        puzzleFunc.moveLeft(puzzleMask, witch, difficulty);
                        break;
                    case 4:
                        movement++;
                        puzzleFunc.moveRight(puzzleMask, witch, difficulty);
                        break;
                }
                ///////
                buf_bitmap = original_bitmap.copy(original_bitmap.getConfig(), false);
                puzzleGPU(buf_bitmap, puzzleMask);
                capturedImageHolder.setImageBitmap(buf_bitmap);
                ///////
            } else {
                Toast.makeText(MainActivity.this, "Seon-Ip-Ryeok non-no", Toast.LENGTH_SHORT).show();
            }
        }
    };

    protected void onResumeGPIO(){
        super.onResume();
    }

    public void onReceiveGPIO(int val){
        Message text = Message.obtain();
        text.arg1=val;
        handler.sendMessage(text);
    }

    //7SEG
    private class SegmentThread extends Thread {
        @Override
        public void run(){
            super.run();
            while(mThreadRun){
                byte[] n={0,0,0,0,0,0,0};

                if(mStart==false){writeDriver7seg(n, n.length);}
                else{
                    for(i=0; i<100; i++){
                        //34 저 값을 1초 기준으로 잡고
                        //n배 될 때마다 n초에 한 번 count down
                        n[0]=(byte) (movement % 1000000 / 100000);
                        n[1]=(byte) (movement % 100000 / 10000);
                        n[2]=(byte) (movement % 10000 / 1000);
                        n[3]=(byte) (movement % 1000 / 100);
                        n[4]=(byte) (movement % 100 / 10);
                        n[5]=(byte) (movement % 10);
                        writeDriver7seg(n, n.length);
                    }
                }
            }
        }
    }

    //LED
    @Override
    protected void onPause() {
        //TODO Auto-generated method stub
        closeDriverLED();

        closeDriver7seg();
        mThreadRun=false;
        mSegThread=null;
        super.onPause();
    }

    @Override
    protected void onResume() {
        //TODO Auto-generated method stub
        if(openDriverLED("/dev/sm9s5422_led")<0){
            Toast.makeText(MainActivity.this, "Driver Open Failed LED", Toast.LENGTH_SHORT).show();
        }

        if(openDriver7seg("/dev/sm9s5422_segment")<0){
            Toast.makeText(MainActivity.this, "Driver Open Failed 7seg", Toast.LENGTH_SHORT).show();
        }
        mThreadRun=true;

        super.onResume();
    }

    //CAM
    public static Camera getCameraInstance(){
        Camera c = null;
        try{
            c=Camera.open();
        } catch (Exception e){
            //Camera is not available (in use or does not exist)
        }
        return c;
    }

    final PictureCallback pictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            int w = options.outWidth;
            int h = options.outHeight;
            Matrix mtx = new Matrix();
            mtx.postRotate(180);//basic option is already rotated 180 degree
            if (bitmap == null) {
                Toast.makeText(MainActivity.this, "Captured image is empty", Toast.LENGTH_LONG).show();
                return;
            }
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
            original_bitmap = scaleDownBitmapImage(rotatedBitmap, 400, 400);
            buf_bitmap=original_bitmap.copy(original_bitmap.getConfig(), false);
            puzzleGPU(buf_bitmap, puzzleMask);
            capturedImageHolder.setImageBitmap(buf_bitmap);
            preview2.setImageBitmap(original_bitmap);
        }

        private Bitmap scaleDownBitmapImage(Bitmap bitmap, int newWidth, int newHeight) {
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }

        //@Override
        protected void onPause() {
            MainActivity.super.onPause();
            releaseMediaRecorder();
            releaseCamera();
        }

        private void releaseMediaRecorder() {
            mCamera.lock();
        }

        private void releaseCamera() {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
        }
    };
}