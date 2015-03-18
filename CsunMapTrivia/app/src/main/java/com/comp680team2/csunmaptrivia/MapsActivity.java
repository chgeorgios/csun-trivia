/* Chris Bowles, Victor Perez, Russell Templet, Nishika Malhotra, Maria Velasquez
 * Comp 680, Team 2, Spring 2015, Prof. Boctor
 * MapsActivity.java
 */

package com.comp680team2.csunmaptrivia;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.comp680team2.controller.GameController;
import com.comp680team2.model.QuestionHolder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    double latitude = 0;
    double longitude = 0;
    double vertX[] = new double[4];
    double vertY[] = new double[4];
    int seconds = 10;
    TextView timerTextView;
    TextView questionTextView;
    Polygon polygon = null;
    String trivia = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_activity);
        timerTextView = (TextView) findViewById(R.id.timer);
        questionTextView = (TextView) findViewById(R.id.questionText);

        initializeGame();

        Thread backgroundThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    synchronized (this) {
                        try {
                            wait(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                timerTextView.setText(Integer.toString(seconds));
                                seconds--;
                            }
                        });
                    }
                }
            }
        });
        backgroundThread.start();
    }

    private void initializeGame() {
        Thread initThread = new Thread(new Runnable() {
            public void run() {
                //show some sort of loading mask
                final QuestionHolder questionHolder = new GameController().fetchQuestionSet();
                //hide the loading mask

                runOnUiThread(new Runnable() {
                    public void run() {
                        questionTextView.setText(questionHolder.getQuestion(0).getText());
                    }
                });
                try {
                    for (int i = 0; i < 4; i++) {
                        double x = questionHolder.getQuestion(0).getAnswer().getCoordinate(i).getX();
                        double y = questionHolder.getQuestion(0).getAnswer().getCoordinate(i).getY();
                        vertX[i] = x;
                        vertY[i] = y;
                        if(questionHolder.getQuestion(0).getTrivia() != null) {
                            trivia = questionHolder.getQuestion(0).getTrivia();
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();

                }
            }
        });
        initThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        double x = 34.240089;
        double y = -118.529435;
        LatLng ll = new LatLng(x, y);
        float zoom = 17;
        final int sides = 4;
        mMap.addMarker(new MarkerOptions().position(ll).title("CSUN"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, zoom));
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

        @Override
        /*
        listener method to capture the long click event on the google map when user
        makes a long press to answer the question
         */
        public void onMapLongClick(final LatLng point) {
              MapsActivity.this.latitude = point.latitude;
              MapsActivity.this.longitude = point.longitude;
              if(!(MapsActivity.this.latitude == 0.0 && MapsActivity.this.longitude == 0.0)){
                   boolean myResult = findInPolygon(sides, vertX, vertY, MapsActivity.this.latitude, MapsActivity.this.longitude);
                   if (myResult) {
                        polygon = mMap.addPolygon(new PolygonOptions()
                                .add(new LatLng(vertX[0], vertY[0]), new LatLng(vertX[1], vertY[1]), new LatLng(vertX[2], vertY[2]),
                                        new LatLng(vertX[3], vertY[3]))
                                .strokeColor(Color.GREEN)
                                .fillColor(Color.BLUE));
                    } else {
                        polygon = mMap.addPolygon(new PolygonOptions()
                                .add(new LatLng(vertX[0], vertY[0]), new LatLng(vertX[1], vertY[1]), new LatLng(vertX[2], vertY[2]),
                                        new LatLng(vertX[3], vertY[3]))
                                .strokeColor(Color.RED)
                                .fillColor(Color.BLUE));
                    }
                  //setting the trivia in place of the question
                  questionTextView.setText(trivia);
                }
            }
        });
    }

    //checks if the clicked coordinates lie within the bounds of the correct location corresponding to the question
    boolean findInPolygon(int side, double coordinateX[], double coordinateY[], double testX, double testY) {
        int i, j;
        boolean flag = false;
        for (i=0; i< side; i++) {
            if(testY == coordinateY[i]) {
                if(testX < coordinateX[0] && testX > coordinateX[2]) {
                    flag = true;
                    return flag;
                } else {
                    for (j=0; j< side; j++) {
                        if(testX == coordinateX[j]){
                            flag = true;
                            return flag;
                        }
                    }
                }
            } else if(testX == coordinateX[i]){
                if(testY > coordinateY[0] && testY < coordinateY[1]){
                    flag = true;
                    return flag;
                } else {
                    for (j=0; j< side; j++) {
                        if(testY == coordinateY[j]){
                            flag = true;
                            return flag;
                        }
                    }
                }
            }
        }

        for (i = 0, j = side-1; i < side; j = i++) {
            if ( ((coordinateY[i]>testY) != (coordinateY[j]>testY)) &&
                    (testX < (coordinateX[j]-coordinateX[i]) * (testY-coordinateY[i]) / (coordinateY[j]-coordinateY[i]) + coordinateX[i]) ) {
                flag = !flag;
            }
        }
        return flag;
    }

// -- Rest Example. Tested to work.
    public class DoPOST extends AsyncTask<String, Void, Boolean> {

        Context mContext = null;
        String strNameToSearch = "";

        //Result data
        String strFirstName;
        String strLastName;
        int intAge;
        int intPoints;

        Exception exception = null;

        DoPOST(Context context, String nameToSearch) {
            mContext = context;
            strNameToSearch = nameToSearch;
        }

        @Override
        protected Boolean doInBackground(String... arg0) {

            try {

                //Setup the parameters
                ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("FirstNameToSearch", strNameToSearch));
                //Add more parameters as necessary

                //Create the HTTP request
                HttpParams httpParameters = new BasicHttpParams();

                //Setup timeouts
                HttpConnectionParams.setConnectionTimeout(httpParameters, 15000);
                HttpConnectionParams.setSoTimeout(httpParameters, 15000);

                HttpClient httpclient = new DefaultHttpClient(httpParameters);
                HttpPost httppost = new HttpPost("http://nullroute.cc/rest_test/login.php");
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();

                String result = EntityUtils.toString(entity);

                // Create a JSON object from the request response
                JSONObject jsonObject = new JSONObject(result);

                //Retrieve the data from the JSON object
                strFirstName = jsonObject.getString("FirstName");
                strLastName = jsonObject.getString("LastName");
                intAge = jsonObject.getInt("Age");
                intPoints = jsonObject.getInt("Points");

            } catch (Exception e) {
                Log.e("ClientServerDemo", "Error:", e);
                exception = e;
            }

            return true;
        }
    }
}