package com.smartloan.smtrick.serviceprovider_laundryapp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.itextpdf.text.factories.GreekAlphabetFactory.getString;
import static com.smartloan.smtrick.serviceprovider_laundryapp.Constant.STATUS_APPROVED;

public class Service_Providers_Requests_Adapter extends RecyclerView.Adapter<Service_Providers_Requests_Adapter.ViewHolder> {

    private Context context;
    private List<Requests> uploads;
    AppSharedPreference appSharedPreference;
    LeedRepository leedRepository;
    private DatePickerDialog mDatePickerDialog;
    String fdate;
    int mHour;
    int mMinute;
    EditText edtDateTime;
    Services_Adapter services_adapter;
    ArrayList<User> users = new ArrayList<>();
    User user;

    public Service_Providers_Requests_Adapter(Context context, List<Requests> uploads) {
        this.uploads = uploads;
        this.context = context;
    }

    public Service_Providers_Requests_Adapter(List<Requests> mUsers) {
        this.uploads = mUsers;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.serviceproviders_request_layout, parent, false);
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Requests request = uploads.get(position);
        appSharedPreference = new AppSharedPreference(holder.userCard.getContext());
        leedRepository = new LeedRepositoryImpl();

        holder.textViewName.setText(request.getDate());
        holder.textViewMobile.setText(request.getUserName());
//        holder.textViewAddress.setText(request.getUserAddress());
        holder.textViewPinCode.setText(request.getUserMobile());
        holder.textViewId.setText(request.getUserPinCode());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Requests");

        holder.CardApprove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                setLeedStatus(request);
                leedRepository.sendRequestToApproved(request, new CallBack() {
                    @Override
                    public void onSuccess(Object object) {

                        leedRepository.readAdmin(new CallBack() {
                            @Override
                            public void onSuccess(Object object) {
                                if (object != null) {
                                    users = (ArrayList<User>) object;
                                    user = users.get(0);
                                    sendFCMPush(user.getTokan());
                                }
                            }

                            private void sendFCMPush(String Token) {

                                String Legacy_SERVER_KEY = "AIzaSyCM5Eb6ZrYBWhzGRSsm5WKYlzlT7BlhuKs";
                                String msg = "New Order From " + appSharedPreference.getName();
                                String title = "New Order Has Been Received";
                                String token = Token;

                                JSONObject obj = null;
                                JSONObject objData = null;
                                JSONObject dataobjData = null;

                                try {
                                    obj = new JSONObject();
                                    objData = new JSONObject();

                                    try {
                                        objData.put("body", msg);
                                        objData.put("title", title);
                                        objData.put("sound", "default");
                                        objData.put("icon", "icon_name"); //   icon_name image must be there in drawable
                                        objData.put("tag", token);
                                        objData.put("priority", "high");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    dataobjData = new JSONObject();
                                    dataobjData.put("text", msg);
                                    dataobjData.put("title", title);

                                    obj.put("to", token);
                                    //obj.put("priority", "high");

                                    obj.put("notification", objData);
                                    obj.put("data", dataobjData);
                                    Log.e("!_@rj@_@@_PASS:>", obj.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, Constants.FCM_PUSH_URL, obj,
                                        new Response.Listener<JSONObject>() {
                                            @Override
                                            public void onResponse(JSONObject response) {
                                                Log.e("!_@@_SUCESS", response + "");
                                            }
                                        },
                                        new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                Log.e("!_@@_Errors--", error + "");
                                            }
                                        }) {
                                    @Override
                                    public Map<String, String> getHeaders() throws AuthFailureError {
                                        Map<String, String> params = new HashMap<String, String>();
                                        params.put("Authorization", "key=" + Legacy_SERVER_KEY);
                                        params.put("Content-Type", "application/json");
                                        return params;
                                    }
                                };
                                RequestQueue requestQueue = Volley.newRequestQueue(holder.CardApprove.getContext());
                                int socketTimeout = 1000 * 60;// 60 seconds
                                RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                                jsObjRequest.setRetryPolicy(policy);
                                requestQueue.add(jsObjRequest);
                            }


                            @Override
                            public void onError(Object object) {

                            }
                        });
                        ref.child(request.getRequestId()).removeValue();
                    }

                    @Override
                    public void onError(Object object) {

                    }
                });


            }

            private void setLeedStatus(Requests user) {
                user.setStatus(STATUS_APPROVED);
                Toast.makeText(holder.CardApprove.getContext(), "Approved Successfully", Toast.LENGTH_SHORT).show();
                updateLeed(user.getRequestId(), user.getLeedStatusMap());
            }

            private void updateLeed(String requestId, Map leedStatusMap) {
                leedRepository.updateRequest(requestId, leedStatusMap, new CallBack() {
                    @Override
                    public void onSuccess(Object object) {

                        leedRepository.readAdmin(new CallBack() {
                            @Override
                            public void onSuccess(Object object) {
                                if (object != null) {
                                    users = (ArrayList<User>) object;
                                    user = users.get(0);
                                    sendFCMPush(user.getTokan());
                                }
                            }

                            private void sendFCMPush(String Token) {

                                String Legacy_SERVER_KEY = "AIzaSyCM5Eb6ZrYBWhzGRSsm5WKYlzlT7BlhuKs";
                                String msg = "New Order From " + appSharedPreference.getName();
                                String title = "New Order Has Been Received";
                                String token = Token;

                                JSONObject obj = null;
                                JSONObject objData = null;
                                JSONObject dataobjData = null;

                                try {
                                    obj = new JSONObject();
                                    objData = new JSONObject();

                                    try {
                                        objData.put("body", msg);
                                        objData.put("title", title);
                                        objData.put("sound", "default");
                                        objData.put("icon", "icon_name"); //   icon_name image must be there in drawable
                                        objData.put("tag", token);
                                        objData.put("priority", "high");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    dataobjData = new JSONObject();
                                    dataobjData.put("text", msg);
                                    dataobjData.put("title", title);

                                    obj.put("to", token);
                                    //obj.put("priority", "high");

                                    obj.put("notification", objData);
                                    obj.put("data", dataobjData);
                                    Log.e("!_@rj@_@@_PASS:>", obj.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, Constants.FCM_PUSH_URL, obj,
                                        new Response.Listener<JSONObject>() {
                                            @Override
                                            public void onResponse(JSONObject response) {
                                                Log.e("!_@@_SUCESS", response + "");
                                            }
                                        },
                                        new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                Log.e("!_@@_Errors--", error + "");
                                            }
                                        }) {
                                    @Override
                                    public Map<String, String> getHeaders() throws AuthFailureError {
                                        Map<String, String> params = new HashMap<String, String>();
                                        params.put("Authorization", "key=" + Legacy_SERVER_KEY);
                                        params.put("Content-Type", "application/json");
                                        return params;
                                    }
                                };
                                RequestQueue requestQueue = Volley.newRequestQueue(holder.CardApprove.getContext());
                                int socketTimeout = 1000 * 60;// 60 seconds
                                RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                                jsObjRequest.setRetryPolicy(policy);
                                requestQueue.add(jsObjRequest);
                            }


                            @Override
                            public void onError(Object object) {

                            }
                        });

                    }


                    @Override
                    public void onError(Object object) {
                        Utility.showLongMessage(holder.CardApprove.getContext(), getString(R.string.server_error));
                    }
                });
            }
        });

        holder.userCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog1 = new Dialog(holder.userCard.getContext());
                dialog1.getWindow().setBackgroundDrawableResource(R.drawable.dialogboxanimation);
                dialog1.setContentView(R.layout.customdialogbox_services);

                RecyclerView serviecRecycle = (RecyclerView) dialog1.findViewById(R.id.services_recycle);
                if (request.getServiceList() != null) {
                    services_adapter = new Services_Adapter(holder.userCard.getContext(), request.getServiceList());
                    serviecRecycle.setAdapter(services_adapter);
                    serviecRecycle.setHasFixedSize(true);
                    serviecRecycle.setLayoutManager(new LinearLayoutManager(holder.userCard.getContext()));
                }
                dialog1.show();
            }
        });

    }

    @Override
    public int getItemCount() {
        return uploads.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textViewName;
        public TextView textViewMobile;
//        public TextView textViewAddress;
        public TextView textViewPinCode;
        public TextView textViewId;
        public CardView userCard;
        public Button Request;
        public CardView CardApprove;


        public ViewHolder(View itemView) {
            super(itemView);

            textViewName = (TextView) itemView.findViewById(R.id.namevalue);
            textViewMobile = (TextView) itemView.findViewById(R.id.user_mobilevalue);
//            textViewAddress = (TextView) itemView.findViewById(R.id.user_addressvalue);
            textViewPinCode = (TextView) itemView.findViewById(R.id.user_pincodevalue);
            textViewId = (TextView) itemView.findViewById(R.id.user_idvalue);
            userCard = (CardView) itemView.findViewById(R.id.card_userid);
            Request = (Button) itemView.findViewById(R.id.request);
            CardApprove = (CardView) itemView.findViewById(R.id.card_view_approve);

        }
    }

    public void reload(ArrayList<Requests> leedsModelArrayList) {
        uploads.clear();
        uploads.addAll(leedsModelArrayList);
        notifyDataSetChanged();
    }
}
