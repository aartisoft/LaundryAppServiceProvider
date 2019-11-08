package com.smartloan.smtrick.serviceprovider_laundryapp;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class Fragment_View_Service_Providers_Requests extends Fragment {

  private RecyclerView ServiceRecycler;
  private ArrayList<MemberVO> catalogList;
  private ProgressDialog progressDialog;
  private Service_Providers_Requests_Adapter adapter;
  private EditText edtSearch;
  DatabaseReference databaseReference;
  String Language;
  private AppSharedPreference appSharedPreference;
  private ArrayList<Requests> service_providers;
  private ArrayList<Requests> service_providers1;

  // int[] animationList = {R.anim.layout_animation_up_to_down};
  int i = 0;

  String number;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_view_service_providers, container, false);

    // getActivity().getActionBar().setTitle("Products");
    appSharedPreference = new AppSharedPreference(getContext());
    progressDialog = new ProgressDialog(getContext());

    ServiceRecycler = (RecyclerView) view.findViewById(R.id.catalog_recycle);
    edtSearch = (EditText) view.findViewById(R.id.search_edit_text);

    catalogList = new ArrayList<>();
    service_providers = new ArrayList<>();
    service_providers1 = new ArrayList<>();

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference reference = firebaseDatabase.getReference();
    reference.child("Requests").addChildEventListener(new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

//            Requests requests = dataSnapshot.getValue(Requests.class);
//            if (requests.getServiceProviderId().equalsIgnoreCase(appSharedPreference.getUserid())) {
//                Toast.makeText(getContext(), "Data changed", Toast.LENGTH_SHORT).show();
            DisplayNotification(getContext(),"New Order has been received");
//            }

        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    });


    getServiceProviders();

    if (isNetworkAvailable()) {
//            Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
    } else {
      Toast.makeText(getContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
    }

    databaseReference = FirebaseDatabase.getInstance().getReference();
    edtSearch.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void afterTextChanged(Editable s) {

        if (!s.toString().isEmpty()) {
//                    setAdapter(s.toString());
        } else {
          /*
           * Clear the list when editText is empty
           * */
          catalogList.clear();
          ServiceRecycler.removeAllViews();
        }

      }
    });


    return view;
  }

    public void DisplayNotification(Context context, String message){
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(getContext(), MainActivity_User.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Uri NOTIFICATION_SOUND_URI = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + BuildConfig.APPLICATION_ID + "/" + R.raw.fillingyourinbox);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "Default")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("New Order")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, mBuilder.build());
    }

  private void getServiceProviders() {
    progressDialog.setMessage("Please wait...");
    progressDialog.show();
    Query query = FirebaseDatabase.getInstance().getReference("Requests").orderByChild("serviceProviderId").equalTo(appSharedPreference.getUserid());

    query.addValueEventListener(valueEventListener);
  }

  ValueEventListener valueEventListener = new ValueEventListener() {
    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
      service_providers.clear();
      progressDialog.dismiss();
      //iterating through all the values in database
      for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
        Requests requests = postSnapshot.getValue(Requests.class);

        if (requests.getStatus() != null) {
          if (requests.getStatus().equalsIgnoreCase(Constant.STATUS_GENERATED)) {
            service_providers.add(requests);
          }
        }
      }

      int size = service_providers.size() - 1;
      service_providers1.clear();
      for (int i = size; i >= 0; i--) {
        service_providers1.add(service_providers.get(i));
      }
      serAdapter(service_providers1);
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
      progressDialog.dismiss();

    }
  };

  private void serAdapter(ArrayList<Requests> leedsModels) {
    if (leedsModels != null) {
      if (adapter == null) {
        adapter = new Service_Providers_Requests_Adapter(getActivity(), leedsModels);
        ServiceRecycler.setAdapter(adapter);
        ServiceRecycler.setHasFixedSize(true);
        ServiceRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        //   onClickListner();
      } else {
        ArrayList<Requests> leedsModelArrayList = new ArrayList<>();
        leedsModelArrayList.addAll(leedsModels);
        adapter.reload(leedsModelArrayList);
      }
    }
  }

  private boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager
            = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }
}