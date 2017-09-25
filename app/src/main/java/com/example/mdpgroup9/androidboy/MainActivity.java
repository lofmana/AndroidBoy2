package com.example.mdpgroup9.androidboy;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    public Menu menu;

    Handler repeatedHandler = new Handler();
    final Handler handler2 = new Handler();

    private TextView status;
    private Button btnConnect;
    private ListView listView;
    private Dialog dialog;
    private TextView tv;
    private Button btnForward;
    private Button btnLeft;
    private Button btnRight;
    private Button btnBack;
    private ImageButton btnExplore;
    private ImageButton btnFast;
    private ImageButton btnA;
    private ImageButton btnB;
    private EditText editCommandA;
    private EditText editCommandB;
    private TextInputLayout inputLayout;
    private ArrayAdapter<String> chatAdapter;
    private ArrayList<String> chatMessages;
    private BluetoothAdapter bluetoothAdapter;
    private TextView textViewStatus;
    private TextView textViewXAxis;
    private TextView textViewYAxis;
    private TextView textViewZAxis;
    private CheckBox checkBoxAccelerometer;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";
    public static String preDevice;

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private ChatController chatController;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;
    List<MapGrid> gridList = new ArrayList<MapGrid>();


    SharedPreferences sharedpreferences;

    private Sensor mySensor;
    private SensorManager SM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        findViewsByIds();

        //check device support bluetooth or not
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
            finish();
        }

        //show bluetooth devices dialog when click connect button
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnConnect.getText() == "Disconnect") {
                    chatController.stop();
                } else if (btnConnect.getText() == "Connect") {
                    showPrinterPickDialog();
                }
            }
        });

        // set chat adapter
        chatMessages = new ArrayList<>();
        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatMessages);
//        listView.setAdapter(chatAdapter);
        buttonFunctions();
        populateMap();


        sharedpreferences = getSharedPreferences("COMMANDS", MODE_PRIVATE);
        String commandA = sharedpreferences.getString("COMMAND_A", "A");
        String commandB = sharedpreferences.getString("COMMAND_B", "B");

        //editCommandA.setText(commandA);
        // editCommandB.setText(commandB);

        Log.d("test", sharedpreferences.getString("COMMAND_A", "A"));


        //create sensor manger
        SM = (SensorManager)getSystemService(SENSOR_SERVICE);

        //accelerate sensor
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Register sensor listener
        SM.registerListener(this , mySensor , SensorManager.SENSOR_DELAY_NORMAL);





    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        double xAxis = event.values[0];
        double yAxis = event.values[1];
        double zAxis = event.values[2];

        textViewXAxis.setText("X :" + event.values[0]);
        textViewYAxis.setText("Y :" + event.values[1]);
        textViewZAxis.setText("Z :" + event.values[2]);


        if((chatController.getState() == 0) || (chatController.getState() == 1) || (checkBoxAccelerometer.isChecked() == true))
        {
            if(xAxis > 6)
            {
                sendMessage("R");
            }
            else if (xAxis < -6)
            {
                sendMessage("L");
            }

            else if (yAxis > 6)
            {
                sendMessage("F");
            }

            else if (yAxis <-3)
            {
                sendMessage("B");
            }
        }





    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //Map
    GridView grid;

    private void populateMap() {
        try {
            grid = (GridView) findViewById(R.id.gridView);
            GridAdapter adapter = null;
            MapGrid mapGrid = null;
            gridList = new ArrayList<MapGrid>();
            for (int i = 0; i < 20; i++) {
                for (int k = 0; k < 15; k++) {
                    mapGrid = new MapGrid(i, k, "");
                    gridList.add(mapGrid);
                }
            }
            adapter = new GridAdapter(this, R.layout.map_adapter, gridList);
            grid.setAdapter(adapter);
            grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    MapGrid object = gridList.get(position);
                    int col = object.getBg();
                    if (col == Color.GREEN) {
                        view.setBackgroundColor(Color.parseColor("#C0C0C0"));
                        object.setBg(Color.parseColor("#C0C0C0"));
                    } else {
                        view.setBackgroundColor(Color.GREEN);
                        object.setBg(Color.GREEN);
                    }
                    gridList.set(position, object);
                    int xpos = position % 15;
                    int ypos = position / 15;
                    Toast.makeText(getBaseContext(), "X:" + xpos + " Y:" + ypos, Toast.LENGTH_SHORT).show();
                }
            });
            Toast.makeText(this, "Map has been generated", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(" poulate map", e.getMessage());
        }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;


        getMenuInflater().inflate(R.menu.waypoint_coordinates, menu);
        getMenuInflater().inflate(R.menu.settings, menu);
        getMenuInflater().inflate(R.menu.main, menu);




        return true;

    }


    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btnScan:
                if ((chatController.getState() == 0) || (chatController.getState() == 1)) {
                    showPrinterPickDialog();


                } else if ((chatController.getState() == 2) || (chatController.getState() == 3)) {
                    chatController.stop();
                    menu.findItem(R.id.btnScan).setIcon(R.drawable.ic_bluetooth_black_24dp);
                }
                return true;

            case R.id.btnSettings:
                showSettings();
                return true;

            case R.id.btnCompass:
                showCompass();
                return true;


            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.

                return super.onOptionsItemSelected(item);

        }
    }


    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatController.STATE_CONNECTED:
                            setStatus("Connected to: " + connectingDevice.getName());

//                            btnConnect.setBackgroundColor(Color.RED);
//                            btnConnect.setText("Disconnect");
                            menu.findItem(R.id.btnScan).setIcon(R.drawable.ic_bluetooth_connected_black_24dp);
                            //  btnConnect.setEnabled(false);
                            break;
                        case ChatController.STATE_CONNECTING:
                            setStatus("Connecting...");
                            //   btnConnect.setEnabled(false);
                            break;
                        case ChatController.STATE_LISTEN:
                        case ChatController.STATE_NONE:
                            setStatus("Not connected");
                            handler2.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    menu.findItem(R.id.btnScan).setIcon(R.drawable.ic_bluetooth_black_24dp);
                                }
                            }, 500);


                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    readMessage = statusValidation(readMessage);
                    tv.setText(readMessage);

                    //   chatMessages.add(connectingDevice.getName() + ":  " + statusValidation(readMessage));
                    //    Log.d("test" , "2" + readMessage);
                    //  chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_DEVICE_OBJECT:
                    connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void showPrinterPickDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_bluetooth);
        dialog.setTitle("Bluetooth Devices");

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        //Initializing bluetooth adapters
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        //locate listviews and attatch the adapters
        ListView listView = (ListView) dialog.findViewById(R.id.pairedDeviceList);
        ListView listView2 = (ListView) dialog.findViewById(R.id.discoveredDeviceList);
        listView.setAdapter(pairedDevicesAdapter);
        listView2.setAdapter(discoveredDevicesAdapter);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryFinishReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryFinishReceiver, filter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesAdapter.add(getString(R.string.none_paired));
        }

        //Handling listview item click event
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                preDevice = address;


                dialog.dismiss();

            }

        });

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                preDevice = address;

                dialog.dismiss();


            }
        });

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void setStatus(String s) {
        status.setText(s);
    }

    private void connectToDevice(String deviceAddress) {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        chatController.connect(device);
    }

    private void findViewsByIds() {
        status = (TextView) findViewById(R.id.status);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        // listView = (ListView) findViewById(R.id.list);
        inputLayout = (TextInputLayout) findViewById(R.id.input_layout);
        tv = (TextView) findViewById(R.id.textViewStatus);
        btnForward = (Button) findViewById(R.id.btnForward);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnLeft = (Button) findViewById(R.id.btnLeft);
        btnRight = (Button) findViewById(R.id.btnRight);
        btnExplore = (ImageButton) findViewById(R.id.btnExplore);
        btnFast = (ImageButton) findViewById(R.id.btnFast);
        btnA = (ImageButton) findViewById(R.id.btnA);
<<<<<<< HEAD
        btnB = (ImageButton) findViewById(R.id.btnB);
=======
        btnB =(ImageButton) findViewById(R.id.btnB);
        textViewXAxis = (TextView)findViewById(R.id.textViewXAxis);
        textViewYAxis = (TextView)findViewById(R.id.textViewYAxis);
        textViewZAxis = (TextView)findViewById(R.id.textViewZAxis);
        checkBoxAccelerometer = (CheckBox)findViewById(R.id.checkBoxAccelerometer);


>>>>>>> refs/remotes/origin/master


        //  View btnSend = findViewById(R.id.btn_send);

//        btnSend.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (inputLayout.getEditText().getText().toString().equals("")) {
//                    Toast.makeText(MainActivity.this, "Please input some texts", Toast.LENGTH_SHORT).show();
//                } else {
//                    //TODO: here
//                    sendMessage(inputLayout.getEditText().getText().toString());
//                    inputLayout.getEditText().setText("");
//                }
//            }
//        });


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    chatController = new ChatController(this, handler, this);
                } else {
                    Toast.makeText(this, "Bluetooth still disabled, turn off application!", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void sendMessage(String message) {
        if (chatController.getState() != ChatController.STATE_CONNECTED) {
            Toast.makeText(this, "Connection was lost!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatController.write(send);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            chatController = new ChatController(this, handler, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (chatController != null) {
            if (chatController.getState() == ChatController.STATE_NONE) {
                chatController.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatController != null)
            chatController.stop();
    }

    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    discoveredDevicesAdapter.add(getString(R.string.none_found));
                }
            }
        }
    };


    private void buttonFunctions() {


        btnForward.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("F");
                tv.setText("Moving Forward");
            }
        }, this));


        btnLeft.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("L");

                tv.setText("Turning Left");
            }
        }, this));


        btnRight.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("R");

                tv.setText("Turning Right");
            }
        }, this));


        btnBack.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("B");
                tv.setText("Reversing");
            }
        }, this));

        btnExplore.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("E");
                tv.setText("Exploring");
            }
        }, this));

        btnFast.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("FP");
                tv.setText("Fastest Path");
            }
        }, this));


        btnA.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(sharedpreferences.getString("COMMAND_A", "A"));
                tv.setText("Command A");
            }
        }, this));


        btnB.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(sharedpreferences.getString("COMMAND_B", "B"));
                tv.setText("Command B");
            }
        }, this));


    }


    public String statusValidation(String text) {
        String status = "";


        if (text.equals("{\"status\":\"turning right\"}")) {
            status = "Turning Right";
        } else if (text.equals("{\"status\":\"turning left\"}")) {
            status = "Turning Left";
        } else if (text.equals("{\"status\":\"moving forward\"}")) {
            status = "Moving Forward";
        } else if (text.equals("{\"status\":\"reversing\"}")) {
            status = "Reversing";
        } else if (text.equals("{\"status\":\"exploring\"}")) {
            status = "Exploring";
        } else if (text.equals("{\"status\":\"fastest path\"}")) {
            status = "Fastest Path";
        } else {
            status = text;
        }

        Log.d("command", text);

        return status;
    }


    private void showSettings() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.settings);
        dialog.setTitle("Settings");


        editCommandA = (EditText) dialog.findViewById(R.id.editCommandA);
        editCommandB = (EditText) dialog.findViewById(R.id.editCommandB);

        editCommandA.setText(sharedpreferences.getString("COMMAND_A", "A"));
        editCommandB.setText(sharedpreferences.getString("COMMAND_B", "B"));

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


        dialog.findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("COMMAND_A", editCommandA.getText().toString());
                editor.putString("COMMAND_B", editCommandB.getText().toString());
                editor.apply();

                dialog.dismiss();
                // Log.d("test" , sharedpreferences.getString("COMMAND_A" , "A"));
                Toast.makeText(getBaseContext(), "Settings Saved", Toast.LENGTH_SHORT).show();
            }

        });

        dialog.show();


    }


    private void showCompass()
    {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.waypoint_coordinates);
        dialog.setTitle("Coordinates");

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();



    }




}