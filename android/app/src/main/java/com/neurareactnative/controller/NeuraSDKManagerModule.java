package com.neurareactnative.controller;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.firebase.iid.FirebaseInstanceId;
import com.neura.resources.authentication.AuthenticateCallback;
import com.neura.resources.authentication.AuthenticateData;
import com.neura.resources.data.PickerCallback;
import com.neura.resources.device.Capability;
import com.neura.resources.device.Device;
import com.neura.resources.device.DevicesRequestCallback;
import com.neura.resources.device.DevicesResponseData;
import com.neura.sdk.callbacks.GetPermissionsRequestCallbacks;
import com.neura.sdk.object.AppSubscription;
import com.neura.sdk.object.AuthenticationRequest;
import com.neura.sdk.object.DisplayTextVariations;
import com.neura.sdk.object.EventDefinition;
import com.neura.sdk.object.Permission;
import com.neura.sdk.service.GetSubscriptionsCallbacks;
import com.neura.sdk.service.SubscriptionRequestCallbacks;
import com.neura.standalonesdk.util.SDKUtils;
import com.neura_react_native.R;

import java.util.ArrayList;
import java.util.List;

//import com.facebook.react.ReactRootView;
//import com.facebook.react.ReactInstanceManager;


// these classes are required for playing the audio

public class NeuraSDKManagerModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext mReactApplicationContext;

    private ArrayList<Permission> mPermissions;

    public NeuraSDKManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactApplicationContext = reactContext;

        /** Copy the permissions you've declared to your application from
         * https://dev.theneura.com/console/edit/YOUR_APPLICATION - permissions section,
         * and initialize mPermissions with them.
         * for example : https://s31.postimg.org/x8phjuza3/Screen_Shot_2016_07_27_at_1.png
         */
        mPermissions = new ArrayList<>(Permission.list(new String[]{
                "presenceAtHome",
                "physicalActivity",
                "userLeftWork",
                "userLeftHome"
        }));
    }

    @Override
    public String getName() {
        return "NeuraSDKManagerAndroid";
    }

    @ReactMethod
    public void initConnection() {
//        Toast.makeText(mReactApplicationContext, "init neura connection", Toast.LENGTH_LONG).show();
        NeuraManager.getInstance().initNeuraConnection(mReactApplicationContext,
                mReactApplicationContext.getResources().getString(R.string.app_uid),
                mReactApplicationContext.getResources().getString(R.string.app_secret));
    }

    /**
     * Authenticate with Neura
     * Receiving unique neuraUserId and accessToken (for external api calls : https://dev.theneura.com/docs/api/insights)
     */
    @ReactMethod
    public void authenticate(final Callback callback) {
//        Toast.makeText(mReactApplicationContext, "android native toast", Toast.LENGTH_LONG).show();
        AuthenticationRequest request = new AuthenticationRequest(mPermissions);
        NeuraManager.getInstance().getClient().authenticate(request, new AuthenticateCallback() {
            @Override
            public void onSuccess(AuthenticateData authenticateData) {
                Log.i(getClass().getSimpleName(), "Successfully authenticate with neura. NeuraUserId = "
                        + authenticateData.getNeuraUserId() + ". AccessToken = " + authenticateData.getAccessToken());
                /**
                 * Go to our push notification guide for more info on how to register receiving
                 * events via firebase https://dev.theneura.com/docs/guide/android/pushnotification.
                 * If you're receiving a 'Token already exists error',make sure you've initiated a
                 * Firebase instance like {@link com.neura.sampleapplication.activities.MainActivity#onCreate(Bundle)}
                 * http://stackoverflow.com/a/38945375/5130239
                 */
                NeuraManager.getInstance().getClient().registerFirebaseToken(getCurrentActivity(),
                        FirebaseInstanceId.getInstance().getToken());

                callback.invoke(authenticateData.getAccessToken(), null);

            }

            @Override
            public void onFailure(int errorCode) {
                Log.e(getClass().getSimpleName(), "Failed to authenticate with neura. Reason : "
                        + SDKUtils.errorCodeToString(errorCode));
            }
        });
    }

    @ReactMethod
    public void disconnect() {
        Toast.makeText(mReactApplicationContext, "logging out", Toast.LENGTH_LONG).show();
        NeuraManager.getInstance().getClient().disconnect();
    }

    @ReactMethod
    public void forgetMe() {
        Toast.makeText(mReactApplicationContext, "forgetting me", Toast.LENGTH_LONG).show();
        Activity currentActivity = mReactApplicationContext.getCurrentActivity();
        if (currentActivity != null)
            NeuraManager.getInstance().getClient().forgetMe(currentActivity, false, null);

    }


    @ReactMethod
    public void getAppPermissions(final Callback callback) {
//        Toast.makeText(mReactApplicationContext, "Getting app permissions", Toast.LENGTH_LONG).show();

        NeuraManager.getInstance().getClient().getAppPermissions(new GetPermissionsRequestCallbacks() {
            @Override
            public void onSuccess(List<Permission> list) throws RemoteException {

                class responsePermission {
                    String name;
                    String neuraId;
                    String displayName;
                    boolean grantedByUser;
                    String description;
                    String reason;
                    ArrayList<EventDefinition> associatedEvents = new ArrayList();
                    DisplayTextVariations displayTextVariations;
                    String state;
                    boolean isActive;
                }
                WritableArray permissionArray = new WritableNativeArray();

                for (int i = 0; i < list.size(); i++) {
                    WritableMap map = new WritableNativeMap();
                    map.putString("name", list.get(i).getName());
                    map.putString("neuraId", list.get(i).getId());
                    map.putString("displayName", list.get(i).getDisplayName());
                    map.putBoolean("grantedByUser", list.get(i).isGrantedByUser());
                    map.putString("description", list.get(i).getDescription());
                    map.putString("reason", list.get(i).getReason());
                    map.putString("state", list.get(i).getState());
                    map.putBoolean("isActive", list.get(i).isActive());

                    permissionArray.pushMap(map);
                }

                callback.invoke(permissionArray, null);
            }

            @Override
            public void onFailure(Bundle bundle, int i) throws RemoteException {
                ArrayList<String> response = new ArrayList<String>();
                response.add(null);

                String failureMessage = "Failure getting permissions";
                response.add(failureMessage);


            }

            @Override
            public IBinder asBinder() {
                return null;
            }
        });
    }

    @ReactMethod
    public void getSubscriptions(final Callback callback) {
//        Toast.makeText(mReactApplicationContext, "Getting app subscriptions", Toast.LENGTH_LONG).show();

        NeuraManager.getInstance().getClient().getSubscriptions(new GetSubscriptionsCallbacks() {
            @Override
            public void onSuccess(List<AppSubscription> list) {
                WritableArray subscriptionArray = new WritableNativeArray();

                for (int i = 0; i < list.size(); i++) {
                    WritableMap map = new WritableNativeMap();
                    map.putString("identifier", list.get(i).identifier);                //was getIdentifier()
                    map.putString("eventName", list.get(i).eventName);                  //was get...
                    map.putString("addId", list.get(i).appId);                          //was get...
                    map.putString("usageDescription", list.get(i).usageDescription);    //was get...
                    map.putString("neuraId", list.get(i).neuraId);                      //was get...
                    System.out.println(list.get(i).identifier);                         //was get...
                    subscriptionArray.pushMap(map);
                }


                callback.invoke(subscriptionArray, null);
            }

            @Override
            public void onFailure(Bundle bundle, int i) {
//                callback.invoke([null, "error"]);
            }
        });
    }

    @ReactMethod
    public void subscribeToEvent(String eventName, final Callback callback) {
        Toast.makeText(mReactApplicationContext, "Getting app subscriptions", Toast.LENGTH_LONG).show();
        System.out.println(eventName);
        NeuraManager.getInstance().getClient().subscribeToEvent(eventName, eventName, true, new SubscriptionRequestCallbacks() {
            @Override
            public void onSuccess(String s, Bundle bundle, String s1) {
                callback.invoke(s, null);
            }

            @Override
            public void onFailure(String s, Bundle bundle, int i) {
                callback.invoke(null, s);
            }
        });
    }

    @ReactMethod
    public void getMissingDataForEvent(String eventName, final Callback callback) {
        Toast.makeText(mReactApplicationContext, "Getting missing data", Toast.LENGTH_LONG).show();

        NeuraManager.getInstance().getClient().getMissingDataForEvent(eventName, new PickerCallback() {
            @Override
            public void onResult(boolean b) {
                if (b == true) {
                    callback.invoke(null, null);
                } else {
                    callback.invoke(null, true);
                }
            }
        });
    }

    @ReactMethod
    public void isMissingDataForEvent(String eventName, final Callback callback) {
        boolean b = NeuraManager.getInstance().getClient().isMissingDataForEvent(eventName);
        callback.invoke(b);
    }

    @ReactMethod
    public void removeSubscription(String eventName, final Callback callback) {
        System.out.println(eventName);
        NeuraManager.getInstance().getClient().removeSubscription(eventName, ("_"+eventName), true, new SubscriptionRequestCallbacks() {
            @Override
            public void onSuccess(String s, Bundle bundle, String s1) {
                callback.invoke(s, null);
            }

            @Override
            public void onFailure(String s, Bundle bundle, int i) {
                callback.invoke(null, s);
            }
        });
    }

    @ReactMethod
    public void addDeviceByCapability(ReadableArray capabilities, final Callback callback) {
        Toast.makeText(mReactApplicationContext, "Add device with capability", Toast.LENGTH_LONG).show();
        ArrayList<String> capabilitiesArray = new ArrayList<>(capabilities.size());
        for (int i = 0; i < capabilities.size(); i++) {
            capabilitiesArray.add(capabilities.getString(i));
        }
        NeuraManager.getInstance().getClient().addDevice(capabilitiesArray, new PickerCallback() {
            @Override
            public void onResult(boolean b) {
                if (b) {
                    callback.invoke(b, null);
                } else {
                    callback.invoke(null, b);
                }
            }
        });
    }

    @ReactMethod
    public void addDeviceByName(String deviceName, final Callback callback) {
        Toast.makeText(mReactApplicationContext, "Add device with name", Toast.LENGTH_LONG).show();
        NeuraManager.getInstance().getClient().addDevice(deviceName, new PickerCallback() {
            @Override
            public void onResult(boolean b) {
                if (b) {
                    callback.invoke(b, null);
                } else {
                    callback.invoke(null, b);
                }
            }
        });
    }

    @ReactMethod
    public void addDevice(final Callback callback) {
        Toast.makeText(mReactApplicationContext, "Add device", Toast.LENGTH_LONG).show();
        NeuraManager.getInstance().getClient().addDevice(new PickerCallback() {
            @Override
            public void onResult(boolean b) {
                if (b) {
                    callback.invoke(b, null);
                } else {
                    callback.invoke(null, b);
                }
            }
        });
    }

    @ReactMethod
    public void getKnownCapabilities(Callback callback) {
        Toast.makeText(mReactApplicationContext, "Known capabilities", Toast.LENGTH_LONG).show();
        ArrayList<Capability> capabilityArray = NeuraManager.getInstance().getClient().getKnownCapabilities();

        WritableArray capabilities = new WritableNativeArray();
        for (Capability capability : capabilityArray) {
            WritableMap capabilityMap = new WritableNativeMap();
            capabilityMap.putString("name", capability.getName());
            capabilityMap.putString("displayName", capability.getDisplayName());

            capabilities.pushMap(capabilityMap);
        }
        callback.invoke(capabilities, null);
    }

    @ReactMethod
    public void getKnownDevices(final Callback callback) {
        Toast.makeText(mReactApplicationContext, "Known devices", Toast.LENGTH_LONG).show();
        NeuraManager.getInstance().getClient().getKnownDevices(new DevicesRequestCallback() {
            @Override
            public void onSuccess(DevicesResponseData devicesResponseData) {
                ArrayList<Device> response = devicesResponseData.getDevices();
                WritableArray deviceNames = new WritableNativeArray();
                for (Device device : response) {
                    deviceNames.pushString(device.getName());
                }
                callback.invoke(deviceNames, null);
            }

            @Override
            public void onFailure(int i) {
                callback.invoke(null, i);
            }
        });
    }

    @ReactMethod
    public void isConnected(final Callback callback) {
        Toast.makeText(mReactApplicationContext, "Is connected", Toast.LENGTH_LONG).show();
        Boolean isConnected = SDKUtils.isConnected(mReactApplicationContext, NeuraManager.getInstance().getClient());
        System.out.println("State of the connection: " + isConnected);
        callback.invoke(isConnected);
    }

    @ReactMethod
    public void hasDeviceWithCapability(String capabilityName, final Callback callback) {
        Boolean hasCapability = NeuraManager.getInstance().getClient().hasDeviceWithCapability(capabilityName);
        System.out.println("the capability name is " + capabilityName + " and the response is " + hasCapability);
        callback.invoke(hasCapability);
    }
}