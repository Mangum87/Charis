package com.charis;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class Inventory extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
    }


    public void makeBarcode(View view)
    {
        String text = ((TextView)findViewById(R.id.txtBarcode)).getText().toString();


        try
        {
            final int size = 200;
            QRCodeWriter write = new QRCodeWriter();
            BitMatrix matrix = write.encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap map = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

            for(int i = 0; i < size; i++)
            {
                for(int j = 0; j < size; j++)
                {
                    map.setPixel(i, j, matrix.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }


            if(map != null)
                ((ImageView)findViewById(R.id.imageView)).setImageBitmap(map);
            else
                Toast.makeText(getApplicationContext(), "Error loading QR code", Toast.LENGTH_SHORT).show();
        }
        catch (WriterException e) { e.printStackTrace(); }
    }
}
