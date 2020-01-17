package com.facesobject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.TextAnnotation;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.IllegalFormatCodePointException;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton fd_1;
    private FloatingActionButton fd_2;
    private ImageView imagen;
    public Vision vision;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("Vision API");

        imagen = (ImageView)findViewById(R.id.image_id);
        fd_1 = (FloatingActionButton) findViewById(R.id.fab_1);
        fd_2 = (FloatingActionButton) findViewById(R.id.fab_2);

        fd_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CargarImagen();
            }
        });

        fd_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TomarFoto();
            }
        });

        Permisos();

        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(), new AndroidJsonFactory(),  null);
        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyCV_ADpVQp5_K1CQ98gc6KeOVq5p1sjqKQ"));
        vision = visionBuilder.build();
    }

    //Permisos de Cámara y Escritura
    private boolean Permisos() {

        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.M){
            return true;
        }

        if((checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED)&&
                (checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)){
            return true;
        }

        if((shouldShowRequestPermissionRationale(CAMERA)) || (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE))){
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,CAMERA},100);
        }else{
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,CAMERA},100);
        }

        return false;
    }

    public void CargarImagen(){
        if (Permisos()){
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/");
            startActivityForResult(intent.createChooser(intent,"Seleccione la Aplicación"),10);
        }

    }

    public void TomarFoto(){
        if (Permisos()){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null){
                startActivityForResult(intent, 0);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK){
            switch (requestCode){
                case 0:
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imagen.setImageBitmap(imageBitmap);
                    break;
                case 10:
                    Uri miPath=data.getData();
                    imagen.setImageURI(miPath);
                    break;
            }
        }
    }


    /////VISION API CLOUD

    public void Button_Click(View view){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                bitmap = scaleBitmapDown(bitmap, 1200);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                byte[] imageInByte = stream.toByteArray();

                //1.- Paso
                Image inputImage = new Image();
                inputImage.encodeContent(imageInByte);
                //2.- Feature
                Feature desiredFeature = new Feature();
                desiredFeature.setType("FACE_DETECTION");
                Feature desiredFeature_1 = new Feature();
                desiredFeature_1.setType("LABEL_DETECTION");
                Feature desiredFeature_2 = new Feature();
                desiredFeature_2.setType("TEXT_DETECTION");

                //3.- Arma la Solicitud(es)

                AnnotateImageRequest request = new AnnotateImageRequest();
                request.setImage(inputImage);
                request.setFeatures(Arrays.asList(desiredFeature_1,desiredFeature, desiredFeature_2));

                BatchAnnotateImagesRequest batchRequest =  new BatchAnnotateImagesRequest();
                batchRequest.setRequests(Arrays.asList(request));

                //4.- Asignamos al Control VisionBuilder la Solicitud
                Vision.Images.Annotate annotateRequest = null;
                try {
                    annotateRequest = vision.images().annotate(batchRequest);

                    //5.- Enviamos la solicitud
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse batchResponse  = annotateRequest.execute();

                    //6.- Obtener resultado de la imagen

                    //Face Detection
                    List<FaceAnnotation> faces = batchResponse.getResponses().get(0).getFaceAnnotations();
                    int numberOfFaces = 0;
                    if (faces != null){
                        numberOfFaces = faces.size();
                    }
                    String likelihoods = "";
                    for(int i=0; i<numberOfFaces; i++) {
                        likelihoods += "\n Es " +  faces.get(i).getJoyLikelihood() + " la cara " + i + " es feliz";
                    }

                    final String result = "Esta foto tiene " + numberOfFaces + " caras" + likelihoods;

                    //Object Detection
                    final StringBuilder message = new StringBuilder("Encontré estas cosas:\n");
                    List<EntityAnnotation> labels = batchResponse.getResponses().get(0).getLabelAnnotations();
                    if (labels != null){
                        for (EntityAnnotation label : labels) {
                            message.append(String.format(Locale.US, "%.3f: %s", label.getScore(), label.getDescription()));
                            message.append("\n");
                        }
                    } else {
                        message.append("Nada");
                    }

                    //Text Detection
                    TextAnnotation text = batchResponse.getResponses().get(0).getFullTextAnnotation();
                    final String result_text = text.getText();

                    //7.- Asignar el resultado

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.text_id);
                            imageDetail.setText(result);
                            TextView imageDetail_2 = (TextView)findViewById(R.id.text_id2);
                            imageDetail_2.setText(message);
                            TextView imageDetail_3 = (TextView)findViewById(R.id.text_id3);
                            imageDetail_3.setText(result_text);
                        }
                    });



                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //Método para reducir la imagen
    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }
}
