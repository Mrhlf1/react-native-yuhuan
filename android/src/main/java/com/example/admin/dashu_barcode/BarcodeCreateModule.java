package com.example.admin.dashu_barcode;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.view.Gravity;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.AsyncTask;

import android.zyapi.pos.PosManager;
import android.zyapi.pos.PrinterDevice;
import android.zyapi.pos.interfaces.OnPrintEventListener;


public class BarcodeCreateModule extends ReactContextBaseJavaModule{

  public BarcodeCreateModule(ReactApplicationContext reactContext) {
      super(reactContext);
  }
    @Override
    public String getName() {
        return "testSdk";
    }

    private PrinterDevice mPrinter = null;
    private Bitmap mBitmap =null;
    private static Promise PROMISE;
    private String InitMsg = "1";
    public static boolean ISFIRSTPRINT = true;//判定是否为第一次打印

    @ReactMethod
    public void init(final Promise promise) {
      PROMISE = promise;

      AsyncTask task = new AsyncTask<Object, Object, Boolean>() {
        @Override
      		protected Boolean doInBackground(Object... params) {
      			return true;
      		}
          @Override
      		protected void onPostExecute(Boolean result) {
            PosManager.get().init(getReactApplicationContext(),"A980");
            mPrinter = PosManager.get().getPrinterDevice();
            try {
              //必须初始化
              mPrinter.init();
              mPrinter.setPrintEventListener(mPrinterListener);
              WritableMap map = Arguments.createMap();
              map.putString("success", InitMsg);
              promise.resolve(map);
            } catch (Exception e) {
                promise.reject(e.getMessage(), e);
            }
          }
      };
      task.execute();
    }

    private void showMsg(String msg) {
        InitMsg = msg;
    }

    private OnPrintEventListener mPrinterListener = new OnPrintEventListener() {
        @Override
        public void onEvent(int event) {
            switch (event) {
                case EVENT_UNKNOW:
                     showMsg("未知错误");
                    break;
                case  EVENT_NO_PAPER:
                     showMsg("打印机缺纸");
                    break;
                case EVENT_PAPER_JAM:
                     showMsg("打印机卡纸");
                    break;
                case EVENT_PRINT_OK:
                     showMsg("打印完成");
                    break;
                case EVENT_HIGH_TEMP:
                     showMsg("机芯温度过热");
                    break;
                case EVENT_LOW_TEMP:
                     showMsg("机芯温度过低");
                    break;
                case EVENT_CONNECTED:
                     showMsg("打印机连接完成");
                    break;
                case EVENT_CONNECT_FAILD:
                     showMsg("打印机连接失败");
                    break;
                case EVENT_STATE_OK:
                     showMsg("打印机状态正常");
                    break;
                case EVENT_CHECKED_BLACKFLAG:
                     showMsg("检测到黑标");
                    break;
                case EVENT_NO_CHECKED_BLACKFLAG:
                     showMsg("未检测到黑标");
                    break;
                case EVENT_TIMEOUT:
                     showMsg("打印机响应超时");
                    break;
                case EVENT_PRINT_FAILD:
                     showMsg("打印失败");
                    break;
                default:
                     showMsg("打印失败:" + event);
                    break;
            }
        }

        @Override
        public void onGetState(int cmd, int state) {

        }

        @Override
        public void onCheckBlack(int event) {
            switch (event) {
                case EVENT_CHECKED_BLACKFLAG:
                     showMsg("检测到黑标");
                    break;
                case EVENT_NO_CHECKED_BLACKFLAG:
                     showMsg("没有检测到黑标");
                    break;
                case EVENT_NO_PAPER:
                     showMsg("检测黑标时缺纸");
                    break;
            }

        }
    };

    @ReactMethod
    public void printCode(final String names , final String code , final String nowdate , final Promise promise) {
      PROMISE = promise;
      AsyncTask task = new AsyncTask<Object, Object, Boolean>() {
        @Override
      		protected Boolean doInBackground(Object... params) {
      			return true;
      		}
          @Override
      		protected void onPostExecute(Boolean result) {
            int concentration = 25 ;//打印浓度
            PrinterDevice.TextData tData_head = mPrinter.new TextData();//构造TextData实例
            if(ISFIRSTPRINT == false) {
                tData_head.addParam("1B4B15");//退步
            }
            else
            {
                tData_head.addParam("1B4A08");//进步
            }
            mPrinter.addText(concentration,tData_head);
            PrinterDevice.TextData  tData_body =  mPrinter.new TextData();//构造TextData实例
            tData_body.addText("品名：" + names + "\n\n");
            tData_body.addText("日期：" + nowdate + "\n\n\n");//添加打印内容
            tData_body.addParam(mPrinter.PARAM_TEXTSIZE_24);//设置两倍字体大小
            mPrinter.addText(concentration,tData_body);//添加到打印队列
            
            int  mWidth = 300;
            int  mHeight =60;
            mBitmap = creatBarcode(code, mWidth, mHeight,true, 1);
            byte[] printData = bitmap2PrinterBytes(mBitmap);
            mPrinter.addBmp(concentration, 30, mBitmap.getWidth(), mBitmap.getHeight(), printData);
            //添加黑标检测 走纸到黑标处再开始打印下一张数据
            mPrinter.addAction(PrinterDevice.PRINTER_CMD_KEY_CHECKBLACK);
            PrinterDevice.TextData tDataEnter = mPrinter.new TextData();//构造TextData实例
            tDataEnter.addText("\n\n");//多输出到撕纸口(print more paper for paper tearing)
            mPrinter.addText(concentration,tDataEnter);//添加到打印队列(add to print queue)
            mPrinter.printStart();//开始队列打印(begin to print)

            WritableMap map = Arguments.createMap();
            map.putString("success", "1");
            promise.resolve(map);

            ISFIRSTPRINT = false;//重置首次打印变量值(reset the first printing variable)

          }
      };
      task.execute();
    }

    public byte[] bitmap2PrinterBytes (Bitmap bitmap)
    {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int startX = 0;
        int startY = 0;
        int offset = 0;
        int scansize = width;
        int writeNo = 0;
        int rgb=0;
        int colorValue = 0;
        int[] rgbArray = new int[offset + (height - startY) * scansize
                + (width - startX)];
        bitmap.getPixels(rgbArray, offset, scansize, startX, startY,
                width, height);

        int iCount = (height % 8);
        if (iCount > 0) {
            iCount = (height / 8) + 1;
        } else {
            iCount = (height / 8);
        }
        byte [] mData = new byte[iCount*width];
        for (int l = 0; l <= iCount - 1; l++) {
            for (int i = 0; i < width; i++) {
                int rowBegin = l * 8;
                int tmpValue = 0;
                int leftPos = 7;
                int newheight = ((l + 1) * 8 - 1);
                for (int j = rowBegin; j <=newheight; j++) {
                    if (j >= height) {
                        colorValue = 0;
                    } else {
                        rgb = rgbArray[offset + (j - startY)* scansize + (i - startX)];
                        if (rgb == -1) {
                            colorValue = 0;
                        } else {
                            colorValue = 1;
                        }
                    }
                    tmpValue = (tmpValue + (colorValue << leftPos));
                    leftPos = leftPos - 1;

                }
                mData[writeNo]=(byte) tmpValue;
                writeNo++;
            }
        }

        return mData;
    }

    @ReactMethod
    public void printClose(final Promise promise) {
      PROMISE = promise;

      AsyncTask task = new AsyncTask<Object, Object, Boolean>() {
        @Override
      		protected Boolean doInBackground(Object... params) {
      			return true;
      		}
          @Override
      		protected void onPostExecute(Boolean result) {
            try {
              mPrinter.close();
              PosManager.get().close();
              WritableMap map = Arguments.createMap();
              map.putString("success", "close");
              promise.resolve(map);
            } catch (Exception e) {
                promise.reject(e.getMessage(), e);
            }
          }
      };
      task.execute();
    }
    /**
     * 图片两端所保留的空白的宽度
     */
    private static int marginW = 20;
    /**
     * 条形码的编码类型
     */
    public static BarcodeFormat barcodeFormat = BarcodeFormat.CODE_128;

    /**
     * 生成条形码
     *
     * @param context
     * @param contents
     *            需要生成的内容
     * @param desiredWidth
     *            生成条形码的宽带
     * @param desiredHeight
     *            生成条形码的高度
     * @param displayCode
     *            是否在条形码下方显示内容
     * @return
     */
    public Bitmap creatBarcode(String contents, int desiredWidth, int desiredHeight, boolean displayCode, int barType)
    {
        Bitmap ruseltBitmap = null;
        if (barType == 1) {
            barcodeFormat = BarcodeFormat.CODE_128;
        } else if (barType == 2) {
            barcodeFormat = BarcodeFormat.QR_CODE;
        }
        if (displayCode) {
            Bitmap barcodeBitmap = null;
            try {
                barcodeBitmap = encodeAsBitmap(contents, barcodeFormat,
                        desiredWidth, desiredHeight);
            } catch (WriterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Bitmap codeBitmap = creatCodeBitmap(contents, desiredWidth,
                    desiredHeight, getReactApplicationContext());
            ruseltBitmap = mixtureBitmap(barcodeBitmap, codeBitmap, new PointF(
                    0, desiredHeight));
        } else {
            try {
                ruseltBitmap = encodeAsBitmap(contents, barcodeFormat,
                        desiredWidth, desiredHeight);
            } catch (WriterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return ruseltBitmap;
    }

    /**
     * 生成显示编码的Bitmap
     *
     * @param contents
     * @param width
     * @param height
     * @param context
     * @return
     */
    public static Bitmap creatCodeBitmap(String contents, int width, int height, Context context)
    {
        TextView tv = new TextView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                width, height);
        tv.setLayoutParams(layoutParams);
        tv.setText(contents);
        //tv.setHeight(48);
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setWidth(width);
        tv.setDrawingCacheEnabled(true);
        tv.setTextColor(Color.BLACK);
        tv.setBackgroundColor(Color.WHITE);
        tv.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());

        tv.buildDrawingCache();
        Bitmap bitmapCode = tv.getDrawingCache();
        return bitmapCode;
    }

    /**
     * 生成条形码的Bitmap
     *
     * @param contents
     *            需要生成的内容
     * @param format
     *            编码格式
     * @param desiredWidth
     * @param desiredHeight
     * @return
     * @throws WriterException
     */
    public static Bitmap encode2dAsBitmap(String contents, int desiredWidth, int desiredHeight, int barType)
    {
        if (barType == 1) {
            barcodeFormat = BarcodeFormat.CODE_128;
        } else if (barType == 2) {
            barcodeFormat = BarcodeFormat.QR_CODE;
        }
        Bitmap barcodeBitmap = null;
        try {
            barcodeBitmap = encodeAsBitmap(contents, barcodeFormat,
                    desiredWidth, desiredHeight);
        } catch (WriterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return barcodeBitmap;
    }

    /**
     * 将两个Bitmap合并成一个
     *
     * @param first
     * @param second
     * @param fromPoint
     *            第二个Bitmap开始绘制的起始位置（相对于第一个Bitmap）
     * @return
     */
    public static Bitmap mixtureBitmap(Bitmap first, Bitmap second, PointF fromPoint)
    {
        if (first == null || second == null || fromPoint == null) {
            return null;
        }

        Bitmap newBitmap = Bitmap.createBitmap(first.getWidth(),
                first.getHeight() + second.getHeight(), Config.ARGB_4444);
        Canvas cv = new Canvas(newBitmap);
        cv.drawBitmap(first, 0, 0, null);
        cv.drawBitmap(second, fromPoint.x, fromPoint.y, null);
        cv.save(Canvas.ALL_SAVE_FLAG);
        cv.restore();

        return newBitmap;
    }

    public static Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int desiredWidth, int desiredHeight) throws WriterException
    {
        final int WHITE = 0xFFFFFFFF; // 可以指定其他颜色，让二维码变成彩色效果
        final int BLACK = 0xFF000000;

        HashMap<EncodeHintType, String> hints = null;
        String encoding = guessAppropriateEncoding(contents);
        if (encoding != null) {
            hints = new HashMap<EncodeHintType, String>(2);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result = writer.encode(contents, format, desiredWidth,
                desiredHeight, hints);
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        // All are 0, or black, by default
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }

    public static boolean saveBitmap2file(Bitmap bmp, String filename) {
        CompressFormat format = Bitmap.CompressFormat.JPEG;
        int quality = 100;
        OutputStream stream = null;
        try {
            stream = new FileOutputStream("/sdcard/" + filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return bmp.compress(format, quality, stream);
    }

}
