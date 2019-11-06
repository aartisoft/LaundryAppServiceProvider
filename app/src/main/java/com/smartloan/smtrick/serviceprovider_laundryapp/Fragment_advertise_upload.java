package com.smartloan.smtrick.serviceprovider_laundryapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

import static android.app.Activity.RESULT_OK;

public class Fragment_advertise_upload extends Fragment implements View.OnClickListener {

    private static final int REQUEST_PICK_IMAGE = 1002;
    String image;

    //constant to track image chooser intent
    private static final int PICK_IMAGE_REQUEST = 234;

    //view objects
    private Button buttonChoose;
    private Button buttonUpload;
    private EditText editTextName;
    private ImageView imageView;
    private EditText Idescription;

    //uri to store file
    private Uri filePath;



    //firebase objects
    private StorageReference storageReference;
    private DatabaseReference mDatabase;

    // NOTE: Removed Some unwanted Boiler Plate Codes
    private OnFragmentInteractionListener mListener;

    public Fragment_advertise_upload() {}



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        View view= inflater.inflate(R.layout.layout_advertiseupload, container, false);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);

        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        buttonChoose = (Button) view.findViewById(R.id.buttonChoose);
        buttonUpload = (Button) view.findViewById(R.id.buttonUpload);
        imageView = (ImageView) view.findViewById(R.id.imageView);
        editTextName = (EditText) view.findViewById(R.id.editText);
        Idescription = (EditText) view.findViewById(R.id.description);
        storageReference = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference(Constants.DATABASE_PATH_UPLOADS);


//          Boolean per = isStoragePermissionGranted();
//        spinnervalue();
//        subspinnervalue();
        buttonChoose.setOnClickListener(this);
        buttonUpload.setOnClickListener(this);


        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (data != null) {
                switch (requestCode) {
                    case REQUEST_PICK_IMAGE:

//                        String path = "file:///storage/emulated/0/Download/Image-5312.jpg";
                        if (data.hasExtra("image_path")) {
                            Uri imagePath = Uri.parse(data.getStringExtra("image_path"));

                            String str = imagePath.toString();
                            String whatyouaresearching = str.substring(0, str.lastIndexOf("/"));
                            image = whatyouaresearching.substring(whatyouaresearching.lastIndexOf("/") + 1, whatyouaresearching.length());
                            String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                            File file = new File(root, image);
                            filePath = Uri.fromFile(file);

                            setImage(filePath);
                        } else {
                            Toast.makeText(getContext(), "no image", Toast.LENGTH_SHORT).show();
                        }

                        break;
                }
            }
        } else {

            System.out.println("Failed to load image");
        }

    }

    private void setImage(Uri imagePath) {

        imageView.setImageURI(imagePath);

    }

    public String getFileExtension(Uri uri) {
        ContentResolver cR = getContext().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }


    private void uploadFile() {
        //checking if file is available
        if (filePath != null) {
            //displaying progress dialog while image is uploading
            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            //getting the storage reference
            final StorageReference sRef = storageReference.child(Constants.STORAGE_PATH_UPLOADS + System.currentTimeMillis() + "." + getFileExtension(filePath));

            //adding the file to reference
            sRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //dismissing the progress dialog
                            progressDialog.dismiss();

                            //displaying success toast
                            Toast.makeText(getContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
                            sRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String downloadurl = uri.toString();


                                    Advertise upload = new Advertise(Idescription.getText().toString().trim(), editTextName.getText().toString().trim(), downloadurl);

                                    String uploadId = mDatabase.push().getKey();
                                    mDatabase.child(uploadId).setValue(upload);
                                }
                            });

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //displaying the upload progress
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        } else {
            //display an error if no file is selected
            Toast.makeText(getContext(), "Please Select a file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == buttonChoose) {

            // showFileChooser();
            pickImage();

        } else if (view == buttonUpload) {

            String name = editTextName.getText().toString().trim();
            String DESC = Idescription.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(getContext(), "Enter Name!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(DESC)) {
                Toast.makeText(getContext(), "Enter Description!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (imageView.getDrawable() == null) {
                Toast.makeText(getContext(), "Image Required!", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadFile();

        }
    }


    public void pickImage() {

        startActivityForResult(new Intent(getContext(), ImagePickerActivity.class), REQUEST_PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (permissions[0].equals(Manifest.permission.CAMERA) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        }
    }




    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            // NOTE: This is the part that usually gives you the error
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
