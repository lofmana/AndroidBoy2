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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    ArrayList<String> listItems=new ArrayList<String>();
    ArrayAdapter<String> adapter1;


    public String cur_direction = "NORTH";
    public int back = 1;
    public int front = 1;
    public int left = 1;
    public int right = 1;
    public Menu menu;
    public int x = 0;
    public int y = 0;
    public int zLight = 0;
    public int wayPointX = 0;
    public int wayPointY = 0;

    Handler repeatedHandler = new Handler();
    final Handler handler2 = new Handler();

    private TextView status;
    private Button btnConnect;
    private Dialog dialog;
    private TextView tv;
    private Button btnRefreshMap;
    private Button btnSelAutoManual;
    private Button btnSetRobot;
    private Button btnSetWayPoint;
    private Button btnForward;
    private Button btnLeft;
    private Button btnRight;
    private Button btnBack;
    private Button btnSaveCor;
    private ImageButton btnExplore;
    private ImageButton btnFast;
    private ImageButton btnA;
    private ImageButton btnB;
    private EditText editTextEnterX;
    private EditText editTextEnterY;
    private EditText editCommandA;
    private EditText editCommandB;
    private ListView listView;
    private TextInputLayout inputLayout;
    private ArrayAdapter<String> chatAdapter;
    private ArrayList<String> chatMessages;
    private BluetoothAdapter bluetoothAdapter;
    private TextView textViewStatus;
    private TextView textViewXAxis;
    private TextView textViewYAxis;
    private TextView textViewZAxis;
    private CheckBox checkBoxAccelerometer;
    private CheckBox checkBoxEvil;

    public boolean AUTO = false; //Auto map
    public String last_status; //Map status
    private boolean boolSetRobot = false;
    private boolean boolSetWayPoint = false;
    private boolean boolExistWayPoint = false;
    public static int setRobotPOS = 271;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";
    public static String preDevice;

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothController bluetoothController;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;
    List<MapGrid> gridList = new ArrayList<MapGrid>();
    GridAdapter adapter;

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
                    bluetoothController.stop();
                } else if (btnConnect.getText() == "Connect") {
                    showPrinterPickDialog();
                }
            }
        });

        chatMessages = new ArrayList<>();
        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatMessages);
        buttonFunctions();
        populateMap();


        sharedpreferences = getSharedPreferences("COMMANDS", MODE_PRIVATE);
        String commandA = sharedpreferences.getString("COMMAND_A", "A");
        String commandB = sharedpreferences.getString("COMMAND_B", "B");
        Log.d("test", sharedpreferences.getString("COMMAND_A", "A"));


        //create sensor manger
        SM = (SensorManager) getSystemService(SENSOR_SERVICE);

        //accelerate sensor
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Register sensor listener
        SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);


    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        double xAxis = event.values[0];
        double yAxis = event.values[1];
        double zAxis = event.values[2];

        if ((bluetoothController.getState() == 0) || (bluetoothController.getState() == 1) || (checkBoxAccelerometer.isChecked() == true)) {

            if (((xAxis > -1) && (xAxis < 1)) && ((yAxis > 9) && (yAxis < 10))) {
                front = 1;
                right = 1;
                back = 1;
                left = 1;
            } else if ((xAxis > 7) && (left == 1)) {
                sendMessage("L");
                left = 0;
            } else if ((xAxis < -7) && (right == 1)) {
                sendMessage("R");
                right = 0;


            } else if ((zAxis < -4) && (back == 1)) {
                sendMessage("B");
                back = 0;

            } else if ((zAxis > 9) && (front == 1)) {
                sendMessage("F");
                front = 0;
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
            MapGrid mapGrid = null;
            gridList = new ArrayList<MapGrid>();
            for (int i = 0; i < 20; i++) {
                for (int k = 0; k < 15; k++) {
                    mapGrid = new MapGrid(i, k, "");
                    gridList.add(mapGrid);
                }
            }
            adapter = new GridAdapter(this, R.layout.map_adapter, gridList);
            setRobot(setRobotPOS);
            grid.setAdapter(adapter);
            grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    MapGrid object = gridList.get(position);
                    int col = object.getBg();
                    int xpos = position % 15;
                    int ypos = position / 15;
                    int zpos = position;
//                    sendMessage("X:" + xpos + " Y:" + ypos + " Z:" + zpos);
                    if (col != R.color.Green && boolSetWayPoint == true && boolExistWayPoint == false) {
                        view.setBackgroundResource(R.color.Green);
                        object.setBg(R.color.Green);
                        boolExistWayPoint = true;
                        boolSetWayPoint = false;
                        int y = swapYvalue(ypos);
//                        sendMessage(xpos + "," + y);
                        wayPointX = xpos;
                        wayPointY = y;
                        Toast.makeText(getBaseContext(), "X:" + xpos + " Y:" + y, Toast.LENGTH_SHORT).show();
                        gridList.set(position, object);
                        btnSetWayPoint.setBackgroundResource(android.R.drawable.btn_default);
                    } else {
                        if (col == R.color.Green) {
                            boolExistWayPoint = false;
                            boolSetWayPoint = false;
                            view.setBackgroundResource(R.color.Silver);
                            object.setBg(R.color.Silver);
                            gridList.set(position, object);
                        }

                    }
                    if (boolSetRobot == true) {
                        if ((position % 15 == 0) || (position % 15 == 14) || (position >= 285 && position <= 299) || (position >= 0 && position <= 14)) {
                            Toast.makeText(getBaseContext(), "Robot position not allowed", Toast.LENGTH_SHORT).show();
                        } else {
                            if (setRobotPOS != -1) {
                                resetRobot(setRobotPOS);
                            }
                            setRobot(position);
                            setRobotPOS = position;
                            btnSetRobot.setBackgroundResource(android.R.drawable.btn_default);
                        }

                    }
                }
            });
            Toast.makeText(this, "Map has been generated", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(" populate map", e.getMessage());
        }


    }

    public void setRobot(int zpos) {

        MapGrid object = gridList.get(zpos);
        object.setBg(R.color.Red);
        gridList.set(zpos, object);
        int idx = (zpos) - 15;
        object = gridList.get(idx);
        object.setBg(R.color.Red);//Head Light
        gridList.set(idx, object);
        idx = (zpos + 1);
        object = gridList.get(idx);
        object.setBg(R.color.Red);
        gridList.set(idx, object);
        idx = (zpos - 1);
        object = gridList.get(idx);
        object.setBg(R.color.Red);
        gridList.set(idx, object);
        idx = (zpos + 1) - 15;
        object = gridList.get(idx);
        object.setBg(R.color.Red);
        gridList.set(idx, object);
        idx = (zpos + 1) + 15;
        object = gridList.get(idx);
        object.setBg(R.color.Red);
        gridList.set(idx, object);
        idx = (zpos - 1) - 15;
        object = gridList.get(idx);
        object.setBg(R.color.Red);
        gridList.set(idx, object);
        idx = (zpos - 1) + 15;
        object = gridList.get(idx);
        object.setBg(R.color.Red);
        gridList.set(idx, object);
        idx = (zpos + 15);
        object = gridList.get(idx);
        object.setBg(R.color.Red);
        gridList.set(idx, object);

        if (cur_direction == "NORTH") {
            idx = (zpos) - 15; //Head Light
            object = gridList.get(idx);
            object.setBg(R.color.Yellow);//Head Light
            gridList.set(idx, object);
        } else if (cur_direction == "SOUTH") {
            idx = (zpos) + 15;
            object = gridList.get(idx);
            object.setBg(R.color.Yellow);//Head Light
            gridList.set(idx, object);
        } else if (cur_direction == "EAST") {
            idx = (zpos) + 1;
            object = gridList.get(idx);
            object.setBg(R.color.Yellow);//Head Light
            gridList.set(idx, object);
        } else if (cur_direction == "WEST") {
            idx = (zpos) - 1;
            object = gridList.get(idx);
            object.setBg(R.color.Yellow);//Head Light
            gridList.set(idx, object);
        }
        adapter.notifyDataSetChanged();
        boolSetRobot = false;
    }

    public void resetRobot(int zpos) {
        if (adapter != null) {
            MapGrid object = gridList.get(zpos);
            object.setBg(R.color.Blue);
            gridList.set(zpos, object);
            int idx = (zpos + 15);
            object = gridList.get(idx);
            object.setBg(R.color.Blue);
            idx = (zpos - 15);
            object = gridList.get(idx);
            object.setBg(R.color.Blue);
            idx = (zpos + 1) + 15;
            object = gridList.get(idx);
            object.setBg(R.color.Blue);
            idx = (zpos + 1) - 15;
            object = gridList.get(idx);
            object.setBg(R.color.Blue);
            idx = (zpos - 1) + 15;
            object = gridList.get(idx);
            object.setBg(R.color.Blue);
            idx = (zpos - 1) - 15;
            object = gridList.get(idx);
            object.setBg(R.color.Blue);
            idx = (zpos - 1);
            object = gridList.get(idx);
            object.setBg(R.color.Blue);
            idx = (zpos + 1);
            object = gridList.get(idx);
            object.setBg(R.color.Blue);

            adapter.notifyDataSetChanged();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;


        getMenuInflater().inflate(R.menu.coordinates, menu);
        getMenuInflater().inflate(R.menu.waypoint_coordinates, menu);
        getMenuInflater().inflate(R.menu.settings, menu);
        getMenuInflater().inflate(R.menu.main, menu);


        return true;

    }


    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btnScan:
                if ((bluetoothController.getState() == 0) || (bluetoothController.getState() == 1)) {
                    showPrinterPickDialog();


                } else if ((bluetoothController.getState() == 2) || (bluetoothController.getState() == 3)) {
                    bluetoothController.stop();
                    menu.findItem(R.id.btnScan).setIcon(R.drawable.ic_bluetooth_black_24dp);
                }
                return true;

            case R.id.btnSettings:
                showSettings();
                return true;

            case R.id.btnCompass:
                showCompass();
                return true;

            case R.id.btnClipboard:
                showClipboard();
                return true;


            default:
                return super.onOptionsItemSelected(item);

        }
    }


    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothController.STATE_CONNECTED:
                            setStatus("Connected to: " + connectingDevice.getName());

                            menu.findItem(R.id.btnScan).setIcon(R.drawable.ic_bluetooth_connected_black_24dp);
                            break;
                        case BluetoothController.STATE_CONNECTING:
                            setStatus("Connecting...");
                            break;
                        case BluetoothController.STATE_LISTEN:
                        case BluetoothController.STATE_NONE:
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
                    CheckDirection();

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
        bluetoothController.connect(device);
    }

    private void findViewsByIds() {
        status = (TextView) findViewById(R.id.status);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        // listView = (ListView) findViewById(R.id.list);
        inputLayout = (TextInputLayout) findViewById(R.id.input_layout);
        tv = (TextView) findViewById(R.id.textViewStatus);
        btnSelAutoManual = (Button) findViewById(R.id.btnSelAutoManual);
        btnRefreshMap = (Button) findViewById(R.id.btnRefreshMap);
        btnSetRobot = (Button) findViewById(R.id.btnSetRobot);
        btnSetWayPoint = (Button) findViewById(R.id.btnSetWayPoint);
        btnForward = (Button) findViewById(R.id.btnForward);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnLeft = (Button) findViewById(R.id.btnLeft);
        btnRight = (Button) findViewById(R.id.btnRight);
        btnSaveCor = (Button) findViewById(R.id.btnSaveCor);
        btnExplore = (ImageButton) findViewById(R.id.btnExplore);
        btnFast = (ImageButton) findViewById(R.id.btnFast);
        btnA = (ImageButton) findViewById(R.id.btnA);
        btnB = (ImageButton) findViewById(R.id.btnB);
        btnB = (ImageButton) findViewById(R.id.btnB);
        textViewXAxis = (TextView) findViewById(R.id.textViewXAxis);
        textViewYAxis = (TextView) findViewById(R.id.textViewYAxis);
        textViewZAxis = (TextView) findViewById(R.id.textViewZAxis);
        checkBoxAccelerometer = (CheckBox) findViewById(R.id.checkBoxAccelerometer);
        checkBoxEvil = (CheckBox) findViewById(R.id.checkBoxEvil);


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    bluetoothController = new BluetoothController(this, handler, this);
                } else {
                    Toast.makeText(this, "Bluetooth still disabled, turn off application!", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void sendMessage(String message) {
        if (bluetoothController.getState() != BluetoothController.STATE_CONNECTED) {
            return;
        }
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            bluetoothController.write(send);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            bluetoothController = new BluetoothController(this, handler, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (bluetoothController != null) {
            if (bluetoothController.getState() == BluetoothController.STATE_NONE) {
                bluetoothController.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothController != null)
            bluetoothController.stop();
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


        checkBoxEvil.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                                                    @Override
                                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                                                        if (checkBoxEvil.isChecked() == true) {
                                                            btnForward.setEnabled(false);
                                                            btnBack.setEnabled(false);
                                                            btnLeft.setEnabled(false);
                                                            btnRight.setEnabled(false);

                                                        } else {
                                                            btnForward.setEnabled(true);
                                                            btnBack.setEnabled(true);
                                                            btnLeft.setEnabled(true);
                                                            btnRight.setEnabled(true);
                                                        }
                                                    }
                                                }
        );


        btnSelAutoManual.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AUTO == false) {
                    AUTO = true;
                    btnSelAutoManual.setText("Auto");
                    btnRefreshMap.setVisibility(View.GONE);
                    refreshMap();
                } else {
                    AUTO = false;
                    btnSelAutoManual.setText("Manual");
                    btnRefreshMap.setVisibility(View.VISIBLE);
                }
            }
        });
        btnRefreshMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("GRID");
                refreshMap();
            }
        });

        btnSetRobot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSetRobot.setBackgroundResource(android.R.drawable.btn_default);
                if (boolSetWayPoint == true) {
                    Toast.makeText(getBaseContext(), "Select your Way Point first dude", Toast.LENGTH_SHORT).show();
                } else if (boolSetRobot == false && boolSetWayPoint == false) {
                    Toast.makeText(getBaseContext(), "Choose Robot Position", Toast.LENGTH_SHORT).show();
                    boolSetRobot = true;
                    btnSetRobot.setBackgroundResource(R.color.Green);
                } else if (boolSetRobot = true) {
                    boolSetRobot = false;
                }


            }
        });

        btnSetWayPoint.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                btnSetWayPoint.setBackgroundResource(android.R.drawable.btn_default);
                if (boolSetRobot == true) {
                    Toast.makeText(getBaseContext(), "Choose your Set Robot Position first dude", Toast.LENGTH_SHORT).show();
                } else if (boolExistWayPoint == true) {
                    Toast.makeText(getBaseContext(), "Way Point is already set", Toast.LENGTH_SHORT).show();
                } else if (boolExistWayPoint == false && boolSetWayPoint == false) {
                    boolSetWayPoint = true;
                    boolExistWayPoint = false;
                    Toast.makeText(getBaseContext(), "Select Way Point on the Map", Toast.LENGTH_SHORT).show();
                    btnSetWayPoint.setBackgroundResource(R.color.Green);

                } else if (boolSetWayPoint) {
                    boolSetWayPoint = false;
                }
            }
        });


        btnForward.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("F");
                tv.setText("Moving Forward");
                // CheckDirection();
            }
        }, this));


        btnLeft.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("L");
                tv.setText("Turning Left");
                // CheckDirection();
            }
        }, this));


        btnRight.setOnTouchListener(new RepeatListener(400, 200, new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("R");
                tv.setText("Turning Right");
                // CheckDirection();
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
                if (boolExistWayPoint == false){
                    Toast.makeText(getBaseContext(),"WayPoint has not been set", Toast.LENGTH_SHORT).show();
                }
                else {
                    sendMessage("FP;"+wayPointX+","+wayPointY);
                    Log.d("FnPointSend","FP;"+wayPointX+","+wayPointY);
                    tv.setText("Fastest Path");
                }

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
        String map2 = "123456";
        String map1 = "";
        try {
            JSONObject obj1 = new JSONObject(text);
            status = (String) obj1.get("explore");
            String text1 = status;

            status = toBinary(status);

            String s1 = status.toString();
            int s2 = (s1.length());
            Log.d("length" ,s2 + "");


            StringBuilder sb = new StringBuilder(status);
            sb.deleteCharAt(302);
            sb.deleteCharAt(302);
            sb.deleteCharAt(0);
            sb.deleteCharAt(0);
            status = sb.toString();
            int s3 = (status.length());
            Log.d("length2" ,s3 + "");
            listItems.add("Part1 :" + text1);
            Log.d("exploreBinary", status);
            last_status = status;
//            resetRobot(setRobotPOS);
            markExplore();

            JSONObject obj2 = new JSONObject(text);
            String gridString = (String) obj2.get("grid");
            String text2 = gridString;
            Log.d("asd", gridString);
            gridString = toBinary(gridString);
            last_status = gridString;
            listItems.add("Part2 :" + text2);
            listItems.add("End of string");
            refreshMap();

            JSONObject obj3 = new JSONObject(text);
            status = obj3.getString("robotPosition");
            Log.d("robotstatus", status);
            String[] array = status.split(",");
            String part1 = array[0];
            StringBuilder sb1 = new StringBuilder(part1);
            sb1.deleteCharAt(0);
            part1 = sb1.toString();
            String part2 = array[1];
            String part3 = array[2];
            Log.d("part1", part1);
            Log.d("part2", part2);

            StringBuilder sb2 = new StringBuilder(part3);
            sb2.deleteCharAt(3);
            sb2.deleteCharAt(2);
            sb2.deleteCharAt(0);
            part3 = sb2.toString();
            Log.d("part3", part3);
            checkWayPoint(Integer.parseInt(part1), ((Integer.parseInt(part2))));
            int zpos = ((15 * (swapYvalue(Integer.parseInt(part2)))) + Integer.parseInt(part1));
            resetRobot(setRobotPOS);
            setRobot(zpos);
            setRobotPOS = zpos;
            if (part3.equals("N")){
                cur_direction = "NORTH";
            }
            else if (part3.equals("S")){
                cur_direction = "SOUTH";
            }
            else if (part3.equals("E")){
                cur_direction = "EAST";
            }
            else if (part3.equals("W")){
                cur_direction = "WEST";
            }
            setRobot(setRobotPOS);



        } catch (Exception e) {
            status = text;
            tv.setText("Map String Grid received");
        }
//        try {
//
//            JSONObject obj2 = new JSONObject(text);
//            status = obj2.getString("grid");
//            status = toBinary(status);
//            listItems.add("Part2 :" + status);
//            listItems.add("End of string");
//            Log.d("obs", status);
//            last_status = status;
//            refreshMap();
//
//
//
//        } catch (Exception e) {
//            status = text;
//            tv.setText("Map String Grid received");
//        }


        if (text.equals("{\"status\":\"turning right\"}") || (text.equals("R"))) {
            status = "Turning Right";
        } else if (text.equals("{\"status\":\"turning left\"}") || (text.equals("L"))) {
            status = "Turning Left";
        } else if (text.equals("{\"status\":\"moving forward\"}") || (text.equals("F"))) {
            status = "Moving Forward";
        } else if (text.equals("{\"status\":\"reversing\"}") || (text.equals("B"))) {
            status = "Reversing";
        } else if (text.equals("{\"status\":\"exploring\"}") || (text.equals("E"))) {
            status = "Exploring";
        } else if (text.equals("{\"status\":\"fastest path\"}") || (text.equals("FP"))) {
            status = "Fastest Path";
        } else if (status.charAt(0) == '0') {
            last_status = status;
            if (AUTO == true) refreshMap();
            status = "Map String received";


//        } else {
//
//
//            try {
//                JSONObject obj = new JSONObject(text);
//                status = obj.getString("robotPosition");
//                Log.d("robotstatus", status);
//                String[] array = status.split(",");
//                String part1 = array[0];
//                StringBuilder sb = new StringBuilder(part1);
//                sb.deleteCharAt(0);
//                part1 = sb.toString();
//                String part2 = array[1];
//                Log.d("part1", part1);
//                Log.d("part2", part2);
//                checkWayPoint(Integer.parseInt(part1), ((Integer.parseInt(part2))));
//                int zpos = ((15 * (swapYvalue(Integer.parseInt(part2)))) + Integer.parseInt(part1));
//                resetRobot(setRobotPOS);
//                setRobot(zpos);
//                setRobotPOS = zpos;
//            } catch (Exception e) {
//
//                status = text;
//            }
        }


        Log.d("command", text);

        return status;
    }

    private void refreshMap() {
        MapGrid grid;
        grid = null;
        int diff = 285;
        if (last_status != null) {
            for (int i = 0; i < last_status.length(); i++) {
                if (i > 0 && i % 15 == 0) diff -= (15);
                int pos = diff + (i % 15);
                int s = Integer.parseInt(String.valueOf(last_status.charAt(i)));
                if (s == 0) {

                    grid = gridList.get(pos);
                    //    grid.setBg(R.color.Silver);

                    Log.e("s=0", String.valueOf(pos));
                } else if (s == 1) {
                    grid = gridList.get(pos);
                    grid.setBg(R.color.Brown);
                    Log.e("s=1", String.valueOf(pos));
                }
                gridList.set(pos, grid);
            }
            Log.d("pos", String.valueOf(setRobotPOS));
            setRobot(setRobotPOS);
            if(boolExistWayPoint){
                int y2 = swapYvalue(wayPointY);
                int zpos = (15 * y2) + wayPointX;
                manualSetWayPoint(zpos);
            }
            setRobot(setRobotPOS);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }



    }

    public int swapYvalue(int y) {
        int value = 19;
        y = value - y;
        return y;
    }

    public static String toBinary(String hex) {
        return new BigInteger("1" + hex, 16).toString(2).substring(1);
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


    private void showCompass() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.waypoint_coordinates);
        dialog.setTitle("Coordinates");
        editTextEnterX = (EditText) dialog.findViewById(R.id.editTextEnterX);
        editTextEnterY = (EditText) dialog.findViewById(R.id.editTextEnterY);

        editTextEnterX = (EditText) dialog.findViewById(R.id.editTextEnterX);
        editTextEnterY = (EditText) dialog.findViewById(R.id.editTextEnterY);

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.btnSaveCor).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (boolExistWayPoint == true) {
                    Toast.makeText(getBaseContext(), "Way Point is already set", Toast.LENGTH_SHORT).show();
                } else {
                    String value = editTextEnterX.getText().toString();
                    x = Integer.parseInt(value);
                    String value2 = editTextEnterY.getText().toString();
                    y = Integer.parseInt(value2);
                    wayPointX = x;
                    wayPointY = y;
                    int y2 = swapYvalue(y);
                    int zpos = (15 * y2) + x;
                    manualSetWayPoint(zpos);
                    boolExistWayPoint = true;
                    dialog.dismiss();
//                    sendMessage(x + "," + y);
                }
            }
        });

        dialog.show();


    }


    public void showClipboard()
    {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.coordinates);
        dialog.setTitle("Clipboard");

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }

        });

        listView = (ListView) dialog.findViewById(R.id.listviewClipeboard);

        adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        listView.setAdapter(adapter1);


        dialog.show();

    }

    public void manualSetWayPoint(int position) {
        MapGrid object = gridList.get(position);
        object.setBg(R.color.Green);
        gridList.set(position, object);

        adapter.notifyDataSetChanged();

    }

    public void CheckDirection() {
//        Log.d("any", tv.getText().toString());
//        if (tv.getText().toString().equals("Turning Left")) {
        switch (cur_direction) {
            case "NORTH":
                //change yellow to east
                LightChecker(-15);
//                    cur_direction = "WEST";
                break;
            case "SOUTH":
                LightChecker(15);
//                    cur_direction = "EAST";
                break;
            case "EAST":
                LightChecker(1);
//                    cur_direction = "NORTH";
                break;
            case "WEST":
                LightChecker(-1);
//                    cur_direction = "SOUTH";
                break;
        }
//        } else if (tv.getText().equals("Turning Right")) {
//            switch (cur_direction) {
//                case "NORTH":
//                    LightChecker(-15, 1);
////                    cur_direction = "EAST";
//                    break;
//                case "SOUTH":
//                    LightChecker(15, -1);
////                    cur_direction = "WEST";
//                    break;
//                case "EAST":
//                    LightChecker(1, 15);
////                    cur_direction = "SOUTH";
//                    break;
//                case "WEST":
//                    LightChecker(-1, -15);
////                    cur_direction = "NORTH";
//                    break;
//            }
//
//        } else if (tv.getText().equals("Moving Forward")) {
//            MapGrid object = gridList.get(setRobotPOS);
//            switch (cur_direction) {
//                case "NORTH":
//                    if (setRobotPOS >= 15 && setRobotPOS <= 29) {
//                        Toast.makeText(getBaseContext(), "Not allowed to move out of map", Toast.LENGTH_SHORT).show();
//                    } else {
//                        resetRobot(setRobotPOS);
//                        setRobotPOS -= 15;
//                        setRobot(setRobotPOS);
//                    }
//                    break;
//                case "SOUTH":
//                    if (setRobotPOS >= 270 && setRobotPOS <= 284) {
//                        Toast.makeText(getBaseContext(), "Not allowed to move out of map", Toast.LENGTH_SHORT).show();
//                    } else {
//                        resetRobot(setRobotPOS);
//                        setRobotPOS += 15;
//                        setRobot(setRobotPOS);
//                    }
//                    break;
//                case "EAST":
//                    if (setRobotPOS % 15 == 13) {
//                        Toast.makeText(getBaseContext(), "Not allowed to move out of map", Toast.LENGTH_SHORT).show();
//                    } else {
//                        resetRobot(setRobotPOS);
//                        setRobotPOS += 1;
//                        setRobot(setRobotPOS);
//                    }
//                    break;
//                case "WEST":
//                    if (setRobotPOS % 15 == 1) {
//                        Toast.makeText(getBaseContext(), "Not allowed to move out of map", Toast.LENGTH_SHORT).show();
//                    } else {
//                        resetRobot(setRobotPOS);
//                        setRobotPOS -= 1;
//                        setRobot(setRobotPOS);
//                    }
//                    break;
//            }

//        }
    }


    public void LightChecker(int newlight) {

        resetRobot(setRobotPOS);
        setRobot(setRobotPOS);
        MapGrid object = gridList.get(setRobotPOS);
//        int idx = (setRobotPOS - 15);
//        object = gridList.get(idx);
//        object.setBg(R.color.Red);
//        gridList.set(idx, object);
//        idx = (setRobotPOS + oldlight);
//        object = gridList.get(idx);
//        object.setBg(R.color.Red);
        int idx = (setRobotPOS + newlight);
        object = gridList.get(idx);
        object.setBg(R.color.Yellow);

        adapter.notifyDataSetChanged();

    }

    public String swapPosition(String status) {

        Log.d("testbefore", status);

        for (int i = 0; i < 300; i++) {
            if (status.charAt(i) == '1') {
                if (i >= 0 || i <= 14) {

                    StringBuilder myString = new StringBuilder(status);
                    myString.setCharAt(i, '0');
                    myString.setCharAt(i + 285, '1');
                    status = myString.toString();
                    Log.d("testafter", status);
                }
            }
        }

        Log.d("testfinal", status);
        return status;

    }

    public void checkWayPoint(int x, int y) {
        if ((x == wayPointX) && (y == wayPointY)) {
            Toast.makeText(this, "Waypoint cleared", Toast.LENGTH_SHORT).show();
            boolExistWayPoint = false;
        }
    }

    private void markExplore() {
        MapGrid grid;
        grid = null;
        int diff = 285;
        if (last_status != null) {
            for (int i = 0; i < last_status.length(); i++) {
                if (i > 0 && i % 15 == 0) diff -= (15);
                int pos = diff + (i % 15);
                int s = Integer.parseInt(String.valueOf(last_status.charAt(i)));
                if (s == 0) {

                    grid = gridList.get(pos);
                    //    grid.setBg(R.color.Silver);

                    Log.e("s=0" , String.valueOf(pos));
                }
                else if (s == 1)
                {
                    grid = gridList.get(pos);
                    grid.setBg(R.color.Blue);
                    Log.e("s=1" , String.valueOf(pos));
                }
                gridList.set(pos, grid);
            }
            Log.d("pos" , String.valueOf(setRobotPOS));
            setRobot(setRobotPOS);

            if (adapter != null) adapter.notifyDataSetChanged();
        }
    }

    public void setExplore(){

    }
}


