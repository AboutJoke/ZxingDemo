package com.fantasy.zxingdemo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.PlanarYUVLuminanceSource;
import com.google.zxing.client.android.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Hashtable;

import static com.google.zxing.client.android.Utils.getPath;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView result;
    private Button btn_open_camera;
    private Button btn_open_gallery;

    private static final int REQUEST_CODE = 234;
    private static final int CAMERA_OK = 567;
    private String photo_path;
    private Bitmap scanBitmap;
    private WindowManager wm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = (TextView) findViewById(R.id.result);
        btn_open_camera = (Button) findViewById(R.id.btn_open_camera);
        btn_open_gallery = (Button) findViewById(R.id.btn_open_gallery);

        btn_open_camera.setOnClickListener(this);
        btn_open_gallery.setOnClickListener(this);

        wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_open_camera:
                callCapture("UTF-8");
                break;
            case R.id.btn_open_gallery:
                photo();
                break;
        }
    }

    private void callCapture(String characterSet) {
        /**
         * 获取屏幕的宽度，并将宽度的2/3作为扫码区宽度
         */
        int width = wm.getDefaultDisplay().getWidth() * 2 / 3;
        Intent intent = new Intent();
        intent.setAction(Intents.Scan.ACTION);
        intent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
        intent.putExtra(Intents.Scan.CHARACTER_SET, characterSet);
        /**
         * WIDTH==HEIGHT 设置方形扫码取景区
         * 取景区的总宽度是屏幕宽度的2/3——适配所有机型
         * */
        intent.putExtra(Intents.Scan.WIDTH, width);
        intent.putExtra(Intents.Scan.HEIGHT, width);//
        intent.setClass(this, CaptureActivity.class);//进入zxing模块中的CaptureActivity
        startActivityForResult(intent, CAMERA_OK);
    }

    private void photo() {
        // 激活系统图库，选择一张图片
        Intent innerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片");
        startActivityForResult(wrapperIntent, REQUEST_CODE);
    }

    /**
     * 解析部分图片
     *
     * @param path 图片路径
     * @return
     */
    protected Result scanningImage(String path) {
        if (TextUtils.isEmpty(path)) {

            return null;

        }
        // DecodeHintType 和EncodeHintType
        Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8"); // 设置二维码内容的编码
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 先获取原大小
        scanBitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false; // 获取新的大小

        int sampleSize = (int) (options.outHeight / (float) 200);

        if (sampleSize <= 0)
            sampleSize = 1;
        options.inSampleSize = sampleSize;
        scanBitmap = BitmapFactory.decodeFile(path, options);

        // --------------解析方法1---PlanarYUVLuminanceSource-将图片转为yuv解析----------

        LuminanceSource source1 = new PlanarYUVLuminanceSource(
                rgb2YUV(scanBitmap), scanBitmap.getWidth(),
                scanBitmap.getHeight(), 0, 0, scanBitmap.getWidth(),
                scanBitmap.getHeight());
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                source1));
        MultiFormatReader reader1 = new MultiFormatReader();
        Result result1;
        try {
            result1 = reader1.decode(binaryBitmap);
            String content = result1.getText();
            Log.e("123content", content);
        } catch (NotFoundException e1) {
            e1.printStackTrace();
        }

        // --------------解析方法2---RGBLuminanceSource-将图片转为rgb解析----------

        RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap);
        BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
        QRCodeReader reader = new QRCodeReader();

        try {

            return reader.decode(bitmap1, hints);

        } catch (NotFoundException | ChecksumException | FormatException e) {

            e.printStackTrace();

        }

        return null;

    }

    /**
     * 中文乱码
     *
     * @return
     */
    private String recode(String str) {
        String format = "";

        try {
            boolean ISO = Charset.forName("ISO-8859-1").newEncoder()
                    .canEncode(str);
            if (ISO) {
                format = new String(str.getBytes("ISO-8859-1"), "GB2312");
                Log.i("1234      ISO8859-1", format);
            } else {
                format = str;
                Log.i("1234      stringExtra", str);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return format;
    }

    /**
     * 将bitmap由RGB转换为YUV
     *
     * @param bitmap 转换的图形
     * @return YUV数据
     */
    public byte[] rgb2YUV(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int len = width * height;
        byte[] yuv = new byte[len * 3 / 2];
        int y, u, v;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int rgb = pixels[i * width + j] & 0x00FFFFFF;

                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;

                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;

                y = y < 16 ? 16 : (y > 255 ? 255 : y);
                u = u < 0 ? 0 : (u > 255 ? 255 : u);
                v = v < 0 ? 0 : (v > 255 ? 255 : v);

                yuv[i * width + j] = (byte) y;
//                yuv[len + (i >> 1) * width + (j & ~1) + 0] = (byte) u;
//                yuv[len + (i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE:
                    String[] proj = {MediaStore.Images.Media.DATA};
                    // 获取选中图片的路径
                    Cursor cursor = this.getContentResolver().query(data.getData(),
                            proj, null, null, null);
                    if (cursor.moveToFirst()) {
                        int column_index = cursor
                                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        photo_path = cursor.getString(column_index);
                        if (photo_path == null) {
                            photo_path = getPath(this,
                                    data.getData());
                        }
                    }
                    cursor.close();
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            Result result = scanningImage(photo_path);
                            if (result == null) {
                                Looper.prepare();
                                Toast.makeText(MainActivity.this, "图片格式有误", Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            } else {
                                String recode = recode(result.toString());
                                Message msg = new Message();
                                msg.obj = recode;
                                handler.sendMessage(msg);
                            }
                        }
                    }).start();
                    break;
                case CAMERA_OK:
                    String result = data.getStringExtra(Intents.Scan.RESULT);
                    String recode = recode(result);
                    Message msg = new Message();
                    msg.obj = recode;
                    handler.sendMessage(msg);
                    break;
            }
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            result.setText(msg.obj.toString());
        }
    };
}
