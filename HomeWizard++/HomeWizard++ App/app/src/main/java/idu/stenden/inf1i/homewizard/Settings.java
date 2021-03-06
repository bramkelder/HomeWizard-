package idu.stenden.inf1i.homewizard;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import android.app.Dialog;
import android.view.View.OnClickListener;

public class Settings extends BaseMqttEventActivity
{
    //fields
    private MqttController mqttController;
    private String serial;

    private boolean adminPinEnabled;
    private String adminPin;
    private int counter;
    private boolean loginEnabled = true;
    private long loginTimestamp;

    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        context = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // -- Start admin pin functionality --

        try
        {
            JSONObject adminPinSettings = Util.readAdminPin(Settings.context);
            adminPinEnabled = adminPinSettings.getBoolean("enabled");
            adminPin = adminPinSettings.getString("pin");

            JSONObject getLoginAttempts = Util.readLoginAttempts(Settings.context);
            loginEnabled = getLoginAttempts.getBoolean("enabled");
            loginTimestamp = getLoginAttempts.getLong("timestamp");
            counter = getLoginAttempts.getInt("attempts");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //  if adminpin is enabled and pin is not empty, show dialog
        if (adminPinEnabled && !adminPin.isEmpty())
        {
            final Dialog login = new Dialog(this);

            login.requestWindowFeature(Window.FEATURE_NO_TITLE);
            login.setContentView(R.layout.login_dialog);
            login.setCanceledOnTouchOutside(false); // makes sure you can not cancel dialog by clicking outside of it
            login.setCancelable(false);

            final Button btnLogin = (Button) login.findViewById(R.id.btnLogin);
            final Button btnCancel = (Button) login.findViewById(R.id.btnCancel);
            final EditText txtPassword = (EditText) login.findViewById(R.id.txtPassword);

            btnLogin.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if (txtPassword.getText().toString().trim().length() > 0)
                    {
                        if (loginEnabled)
                        {
                            if (txtPassword.getText().toString().equals(adminPin))
                            {
                                login.dismiss();
                                Util.saveLoginAttempts(context, 0, 2, true); // On login, reset login-attempts
                            }
                            else if (counter == 0)
                            {
                                Toast.makeText(Settings.this, "Login disabled. To many failed attempts. Try again in 60 seconds.", Toast.LENGTH_LONG).show();
                                Util.saveLoginAttempts(context, new Date().getTime(), 0, false); // set login to false and attempts to 0
                                finish();
                            }
                            else
                            {
                                Toast.makeText(Settings.this, "Incorrect pin - " + counter + " attempt(s) left", Toast.LENGTH_LONG).show();
                                // subtract 1 from current counter, and save
                                counter--;
                                Util.saveLoginAttempts(context, 0, counter, true);
                            }
                        }
                        else
                        {
                            // Very simple anti-brute force system.
                            long timespan = Math.abs((System.currentTimeMillis() - 60000 - loginTimestamp) / 1000); // display timer for when login is re-enabled

                            // when 60 seconds passed (in miliseconds), re-enable login
                            if (System.currentTimeMillis() - 60000 > loginTimestamp)
                            {
                                Util.saveLoginAttempts(context, 0, 2, true); // set login to true and reset attempts
                                Toast.makeText(Settings.this, "Login attempts resetting.", Toast.LENGTH_LONG).show();
                                finish();
                            }
                            else
                            {
                                Toast.makeText(Settings.this, "Login is disabled for " + timespan + " seconds.", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    else
                    {
                        Toast.makeText(Settings.this, "Please enter a pin code", Toast.LENGTH_LONG).show();
                    }
                }
            });
            btnCancel.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    finish();
                }
            });
            login.show();
        }

        // this will control the saving and removing of the admin pin-code.
        final Switch adminPinButton = (Switch) findViewById(R.id.adminEnabled);
        final EditText adminPinTxt = (EditText) findViewById(R.id.editAdminPin);
        final Button applyPin = (Button) findViewById(R.id.applyAdminPin);
        applyPin.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    if (!adminPinTxt.getText().toString().isEmpty())
                    {
                        Util.saveAdminPin(Settings.context, adminPinTxt.getText().toString(), true);
                        Toast.makeText(Settings.this, "Pin code saved", Toast.LENGTH_LONG).show();
                    }
                    else if (adminPinTxt.getText().toString().isEmpty() && adminPinButton.isChecked())
                    {
                        Toast.makeText(Settings.this, "Please enter a code", Toast.LENGTH_LONG).show();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        // this will disable inputs and buttons when they shouldn't be used. Triggered when enable switch is clicked.
        adminPinButton.setOnCheckedChangeListener((new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    adminPinTxt.setEnabled(true); // enable pin text input
                    applyPin.setEnabled(true); // enable apply pin button
                }
                else
                {
                    if (!adminPin.isEmpty())
                    {
                        Util.saveAdminPin(Settings.context, "", false);
                        Toast.makeText(Settings.this, "Pin code disabled", Toast.LENGTH_LONG).show();
                    }
                    adminPinTxt.setEnabled(false); // disable pin text input
                    applyPin.setEnabled(false); // disable apply pin button
                }
            }
        }));

        // this will disable inputs and buttons when it shouldn't be used based on config from json file
        if (adminPinEnabled)
        {
            adminPinTxt.setEnabled(true);       // enable pin text input
            applyPin.setEnabled(true);          // enable apply pin button
            adminPinButton.setChecked(true);    // enable the "enable" switch
        }
        else
        {
            adminPinTxt.setEnabled(false);      // disable pin text input
            applyPin.setEnabled(false);         // disable apply pin button
            adminPinButton.setChecked(false);   // disable the "enable" switch
        }

        // -- end admin pin functionality --

        mqttController = MqttController.getInstance();

        //geef text velden aan.
        final EditText emailField = (EditText) findViewById(R.id.emailField);
        final EditText passwordField = (EditText) findViewById(R.id.passwordField);
        final EditText brokerIP = (EditText) findViewById(R.id.brokerIP);
        final EditText brokerPort = (EditText) findViewById(R.id.brokerPort);
        final EditText brokerUser = (EditText) findViewById(R.id.brkUsername);
        final EditText brokerPass = (EditText) findViewById(R.id.brkPassword);
        final CheckBox brokerCrt = (CheckBox) findViewById(R.id.brkUseCrt);

        try
        {
            JSONObject loginSettingsFile = Util.readLoginData(this);
            emailField.setText(loginSettingsFile.getString("email"));
            passwordField.setText(loginSettingsFile.getString("password"));

            JSONObject brokerSettings = Util.readBrokerData(Settings.context);
            brokerIP.setText(brokerSettings.getString("ip"));
            brokerPort.setText(brokerSettings.getString("port"));
            brokerUser.setText(brokerSettings.getString("username"));
            brokerPass.setText(brokerSettings.getString("password"));
            brokerCrt.setChecked(brokerSettings.getBoolean("crt"));

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //set button functionaliteit
        Button loginbutton = (Button) findViewById(R.id.loginButton);
        loginbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //publish email/password
                if (!emailField.getText().toString().isEmpty() && !passwordField.getText().toString().isEmpty())
                {
                    mqttController.loginHomeWizard(emailField.getText().toString(), passwordField.getText().toString(), Settings.context);
                }
                else
                {
                    Toast.makeText(Settings.this, "Username and password field cannot be empty.", Toast.LENGTH_LONG).show();
                }
            }
        });

        Button brokerSettings = (Button) findViewById(R.id.bkrConfirmBtn);
        brokerSettings.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //publish email/password
                Util.saveBrokerData(Settings.context, brokerIP.getText().toString(), brokerPort.getText().toString(), brokerUser.getText().toString(), brokerPass.getText().toString(), brokerCrt.isChecked());
                try
                {
                    JSONObject file = Util.readBrokerData(Settings.context);
                    mqttController.connect(file.getString("ip") + ":" + file.getString("port"), "Homewizard++", file.getString("username"), file.getString("password"), Settings.context);
                }
                catch (JSONException e)
                {
                    Toast toaster = Toast.makeText(getApplicationContext(), "Unable to connect to broker", Toast.LENGTH_SHORT);
                    toaster.show();
                    e.printStackTrace();
                }
            }
        });

        //set button functionaliteit
        Button clearbutton = (Button) findViewById(R.id.clearBtn);
        clearbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Util.saveLoginData(Settings.context, emailField.getText().toString(), passwordField.getText().toString());
                emailField.setText("");
                passwordField.setText("");
                Toast toast = Toast.makeText(getApplicationContext(), "Cleared all login data", Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        //Load stuff from files
        final EditText emailField = (EditText) findViewById(R.id.emailField);
        final EditText passwordField = (EditText) findViewById(R.id.passwordField);
        final EditText brokerIP = (EditText) findViewById(R.id.brokerIP);
        final EditText brokerPort = (EditText) findViewById(R.id.brokerPort);
        final EditText brokerUser = (EditText) findViewById(R.id.brkUsername);
        final EditText brokerPass = (EditText) findViewById(R.id.brkPassword);
        final CheckBox brokerCrt = (CheckBox) findViewById(R.id.brkUseCrt);

        try
        {
            JSONObject loginSettingsFile = Util.readLoginData(this);
            serial = loginSettingsFile.getString("serial");
            emailField.setText(loginSettingsFile.getString("email"));
            passwordField.setText(loginSettingsFile.getString("password"));

            JSONObject brokerSettings = Util.readBrokerData(Settings.context);
            brokerIP.setText(brokerSettings.getString("ip"));
            brokerPort.setText(brokerSettings.getString("port"));
            brokerUser.setText(brokerSettings.getString("username"));
            brokerPass.setText(brokerSettings.getString("password"));
            brokerCrt.setChecked(brokerSettings.getBoolean("crt"));

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void addEventListeners()
    {
        //als er een bericht terug word ontvangen
        final EditText emailField = (EditText) findViewById(R.id.emailField);
        final EditText passwordField = (EditText) findViewById(R.id.passwordField);

        addEventListener(new MqttControllerMessageCallbackListener()
        {
            @Override
            /// Stores last successful login data and serial
            public void onMessageArrived(String topic, MqttMessage message)
            {
                if (topic.equals("HYDRA/AUTH/results"))
                {
                    //haal serial code uit json bericht
                    JSONObject json = null;
                    try
                    {
                        json = new JSONObject(message.toString());
                        if (json.getString("status").equals("ok"))
                        {
                            Util.saveLoginData(Settings.context, emailField.getText().toString(), passwordField.getText().toString());
                        }
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
