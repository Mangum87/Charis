package com.charis;

import androidx.appcompat.app.AppCompatActivity;
import androidx.print.PrintHelper;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class Inventory extends AppCompatActivity
{
    private static final int SIZE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
    }


    public void makeBarcode(View view)
    {
        String text = ((TextView)findViewById(R.id.txtBarcode)).getText().toString();

        if(text.length() < 1)
            return;


        try
        {
            QRCodeWriter write = new QRCodeWriter();
            BitMatrix matrix = write.encode(text, BarcodeFormat.QR_CODE, SIZE, SIZE);
            Bitmap map = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);

            for(int i = 0; i < SIZE; i++)
            {
                for(int j = 0; j < SIZE; j++)
                {
                    map.setPixel(i, j, matrix.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }

            ((ImageView)findViewById(R.id.imageView)).setImageBitmap(map);
        }
        catch (WriterException e) { e.printStackTrace(); }
    }


    public void print(View view)
    {
        TextView text = (TextView)findViewById(R.id.txtBarcode);
        if(text.getText().length() > 0)
        {
            Bitmap map = ((BitmapDrawable) ((ImageView) findViewById(R.id.imageView)).getDrawable()).getBitmap();
            print(map);
        }
        else
            Toast.makeText(this, "No barcode to print", Toast.LENGTH_SHORT).show();
    }


    private void print(Bitmap map)
    {
        PrintHelper printer = new PrintHelper(this);
        printer.setColorMode(PrintHelper.COLOR_MODE_MONOCHROME);
        printer.setOrientation(PrintHelper.ORIENTATION_PORTRAIT);
        printer.setScaleMode(PrintHelper.SCALE_MODE_FIT);

        printer.printBitmap("Test Print", map);
    }
}
