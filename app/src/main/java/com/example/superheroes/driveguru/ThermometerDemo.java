package com.example.superheroes.driveguru;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import io.relayr.LoginEventListener;
import io.relayr.RelayrSdk;
import io.relayr.model.TransmitterDevice;
import io.relayr.model.User;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;


public class ThermometerDemo extends Activity implements LoginEventListener {


    /**
     * Once the Activity has been started, the onCreate method will be called
     * @param savedInstanceState
     */

    private TextView mWelcomeTextView;
    private TextView mTemperatureValueTextView;
    private TextView mTemperatureNameTextView;
    private TransmitterDevice mDevice;
    private Subscription mUserInfoSubscription = Subscriptions.empty();
    private Subscription mTemperatureDeviceSubscription = Subscriptions.empty();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //we load the layout xml defined in app/src/main/res/layout
        View view = View.inflate(this, R.layout.activity_thermometer_demo, null);

        mWelcomeTextView = (TextView) view.findViewById(R.id.txt_welcome);
        mTemperatureValueTextView = (TextView) view.findViewById(R.id.txt_temperature_value);
        mTemperatureNameTextView = (TextView) view.findViewById(R.id.txt_temperature_name);

        setContentView(view);

        //we use the relayr SDK to see if the user is logged in by
        //caling the isUserLoggedIn function
        if (RelayrSdk.isUserLoggedIn()) {
            updateUiForALoggedInUser();
        } else {
            updateUiForANonLoggedInUser();
            logIn();
        }


    }

    /**
     * When Android is ready to draw any menus it initiates the
     * "prepareOptionsMenu" event, this method is caled to handle that
     * event.
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        //remove any previous items from the menu
        menu.clear();

        //Check to see if the user is logged in
        if (RelayrSdk.isUserLoggedIn()) {

            //if the user is logged in, we ask Android to draw the menu
            //we defined earlier in the thermometer_demo_logged_in.xml
            //file
            getMenuInflater().inflate(R.menu.thermometer_demo_logged_in, menu);
        } else {

            //otherwise we return the
            //thermometer_demo_not_logged_in.xml file
            getMenuInflater().inflate(R.menu.thermometer_demo_not_logged_in, menu);
        }

        //we must return this, so that any other classes interested in
        //the prepare menu event can do something.
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * When a menu item is selected, we see which item was called and
     * decide what to do according to the item.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //if the user slected login
        if (item.getItemId() == R.id.action_log_in) {

            //we call the login method on the relayr SDK
            Toast.makeText(this, "Attempting to Login", Toast.LENGTH_SHORT).show();
            RelayrSdk.logIn(this, this);
            return true;
        } else if (item.getItemId() == R.id.action_log_out) {

            //otherwise we call the logout method defined later in this
            //class
            logOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the user logs out
     */
    private void logOut() {

        //call the logOut method on the reayr SDK
        RelayrSdk.logOut();

        //call the invalidateOptionsMenu this is defined in the
        //Activity class and is used to reset the menu option
        invalidateOptionsMenu();

        //use the Toast library to display a message to the user
        Toast.makeText(this, R.string.successfully_logged_out, Toast.LENGTH_SHORT).show();
    }

    /**
     * When a user successfuly logs in, the SuccessUserLogin event is
     * fired, and is handled here:
     */
    @Override
    public void onSuccessUserLogIn() {

        //use the Toast library to display a message to the user
        Toast.makeText(this, R.string.successfully_logged_in, Toast.LENGTH_SHORT).show();
        //call the invalidateOptionsMenu, which is defined in the
        //Activity class and is used to reset the menu option
        invalidateOptionsMenu();
    }

    /**
     * if there is a problem logging in a user, the ErrorLogin evet
     * is initiated and handled here.
     */
    @Override
    public void onErrorLogin(Throwable e) {
        //use the Toast library to display a message to the user
        Toast.makeText(this, R.string.unsuccessfully_logged_in, Toast.LENGTH_SHORT).show();
    }

    private void updateUiForANonLoggedInUser() {
        mTemperatureValueTextView.setVisibility(View.GONE);
        mTemperatureNameTextView.setVisibility(View.GONE);
        mWelcomeTextView.setText(R.string.hello_relayr);
    }

    private void updateUiForALoggedInUser() {
        mTemperatureValueTextView.setVisibility(View.VISIBLE);
        mTemperatureNameTextView.setVisibility(View.VISIBLE);
        loadUserInfo();
    }

    private void loadUserInfo() {
        mUserInfoSubscription = RelayrSdk.getRelayrApi().getUserInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<User>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        showToast(R.string.something_went_wrong);
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(User user) {
                        String hello = String.format(getString(R.string.hello), user.getName());
                        mWelcomeTextView.setText(hello);
                        loadTemperatureDevice(user);
                    }
                });

    }



}