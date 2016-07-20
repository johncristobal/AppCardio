package com.example.electroheart;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;


import android.app.Activity;
//import android.support.v7.app.ActionBarActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

	public boolean flag = true;
	public float x=0,y=0;
	
	public String f1 = "10";
	
	public LinearLayout grafica;
	private BluetoothAdapter btAdapter;
	private BluetoothSocket btSocket;
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static String address = "00:06:66:71:5C:96";
//	private static String address = "20:14:10:31:02:01";	// Para el 1er. bluetooth Chino HC-05
//	private static String address = "20:14:10:31:02:52";	// Para el 2O. bluetooth Chino HC-05
    //public DataInputStream mmInStream;
    //p0ublic DataOutputStream mmOutStream;
    public BufferedReader mmInStream;
    public DataOutputStream mmOutStream;

    private String TAG = "Heart";
    private double time =0, timecoord=0;
    private long time1;
    
    //public float x=0;
    
    private ChartTask hilo = new ChartTask();
    
	private GraphicalView mChart;	
	private XYSeries salida,scalon ;
	private XYMultipleSeriesDataset dataset;	
	private XYSeriesRenderer grafica_1, grafica_2;
	private XYMultipleSeriesRenderer multiRenderer;
	
	public int xMax = 5;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		grafica=(LinearLayout)findViewById(R.id.li);

//----------------------------------Bluetooth-------------------------------------------------------------------------------------------------------
		btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
		//Si esta encendido, es todo, iniciamos conexion bluetooth
		if (btAdapter.isEnabled()) 
		{		
			Log.d("B2", "...Bluetooth ON..."+btAdapter.toString());
		    //Busca dispositivo segun Mac
		    BluetoothDevice device = btAdapter.getRemoteDevice(address);
		     
		    try {
		        btSocket = createBluetoothSocket(device);
		    } catch (IOException e) {
		        errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
		    }
		    
		    //Cancela busqueda de dispositivos
		    btAdapter.cancelDiscovery();
		    
		    //Conecta
		    Log.d(TAG, "...Connecting...");
		    try {
		      btSocket.connect();
		      Log.d(TAG, "....Connection ok...");
		    } catch (IOException e) {
		      try {
		        btSocket.close();
		      } catch (IOException e2) {
		        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
		      }
		    }
		      
		    //Crea flujos de datos para enviar y recibir info.
		    Log.d(TAG, "...Create Socket...");
	        //DataInputStream tmpIn = null;
	        BufferedReader tmpIn = null;
	        DataOutputStream tmpOut = null;

	        try {
	            //tmpIn = new DataInputStream(btSocket.getInputStream());
	            tmpIn = new BufferedReader(new InputStreamReader(btSocket.getInputStream()));
	            tmpOut = new DataOutputStream(btSocket.getOutputStream());
	        } catch (IOException e) { }
	  
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	        
		    Log.d(TAG, "...Flujos Listos...");
		}
		//else{//Prendemos bluetooth, pero ahora no :)}
		
//------------------------------------------------Gráfica-------------------------------------------------------------------------------------------
    	//Crea XYSeries para colocar valores a la gráfica (x, y).
    	salida = new XYSeries("Grafica salida");    	    	
    	scalon = new XYSeries("Grafica escalon");
    	//Crea dataset para almacenar XYSeries
    	dataset = new XYMultipleSeriesDataset();
    	// Adding Visits Series to the dataset
    	dataset.addSeries(salida);    	    	
    	dataset.addSeries(scalon);
    	
    	// Crea XYSeriesRenderer para modificar la grafica en general
    	grafica_1 = new XYSeriesRenderer();
    	grafica_1.setColor(Color.WHITE);
    	//visitsRenderer.setPointStyle(PointStyle.CIRCLE);
    	grafica_1.setFillPoints(true);
    	grafica_1.setLineWidth(2);
    	//visitsRenderer.setDisplayChartValues(true);
    	grafica_2 = new XYSeriesRenderer();
    	grafica_2.setColor(Color.RED);
    	//Renderer.setPointStyle(PointStyle.CIRCLE);
    	grafica_2.setFillPoints(true);
    	grafica_2.setLineWidth(2);    	
    	//Renderer.setDisplayChartValues(true);
    	    	
    	// Crea XYMultipleSeriesRenderer para modificar aspectos generales de la gráfica, como el marco
    	multiRenderer = new XYMultipleSeriesRenderer();    	
    	multiRenderer.setChartTitle("E C G");
    	multiRenderer.setXTitle("Datos");
    	multiRenderer.setYTitle("Datos");
    	multiRenderer.setZoomButtonsVisible(true);    	
    	multiRenderer.setXAxisMin(0);
    	multiRenderer.setXAxisMax(xMax); 
    	//Supongo que el YMax dependera de la amplitud :)
    	//multiRenderer.setYAxisMin(-5e-4);
    	//multiRenderer.setYAxisMax(5e-4);
    	multiRenderer.setYAxisMin(-200);
    	multiRenderer.setYAxisMax(100);
    	multiRenderer.setInScroll(true);
    	//multiRenderer.setBarSpacing(2);
    	
    	// Agrega visitsRenderer to multiRenderer ... parecido a graphicalView con add series.
    	//A la grafica en general agrega los detalles
    	multiRenderer.addSeriesRenderer(grafica_1);
    	multiRenderer.addSeriesRenderer(grafica_2);

   		//Esta funcion coloca si es de barras. pero puede ser otras opciones
    	//mChart = (GraphicalView) ChartFactory.getBarChartView(getBaseContext(), dataset, multiRenderer, Type.DEFAULT);
    	//Con esta la grafica es una linea
    	mChart = (GraphicalView) ChartFactory.getLineChartView(getBaseContext(), dataset, multiRenderer);  
    	grafica.addView(mChart);
    	
    	//Iniciamos time1
    	time1 = System.currentTimeMillis();   
    	//Iniciamos-..pero con el boton
    	//hilo.execute();
	}

    private void errorExit(String title, String message){
	    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
	    finish();
	}
  
	private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException 
	{
		if(Build.VERSION.SDK_INT >= 10){
		  try {
		      final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
		      return (BluetoothSocket) m.invoke(device, MY_UUID);
		  } catch (Exception e) {
		      Log.e(TAG, "Could not create Insecure RFComm Connection",e);
		      }
		  }
		  return  device.createRfcommSocketToServiceRecord(MY_UUID);
	}

//----------------------------------Opciones para el menu------------------------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		/*if (id == R.id.action_settings) {
			return true;
		}*/
		return super.onOptionsItemSelected(item);
	}

//-------------------------------------------Funciones para los botones-----------------------------------------------------------------------------
	//Abrir el flujo de datos del Arduino
	public void abrir(View v)
	{
		hilo.execute();
		flag = true;
		try {
			mmOutStream.write(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	//Cerrar el flujo de datos del Arduino y limpiar grafica	
	public void cerrar(View c) throws IOException
	{
		flag = false;
		try {
			mmOutStream.write(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		hilo.cancel(true);
		hilo = new ChartTask();
	}

//----------------------------------------private class---------------------------------------------------------------
//Esto en otro archivo...
	private class ChartTask extends AsyncTask<Void, String, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			try{		
				Log.d("doIn","Entrando al doIn");
				while(flag)
				{
					//byte [] tam1 = new byte[10];
					//byte [] tam2 = new byte[20];
					
					//long time2 = System.currentTimeMillis();
					//double fin = (double)((time2-time1)/1000.0);
					//time += fin;
					//timecoord += fin;
					
//					float x = (float) (timecoord);
					/*
					int lead=0;					
					lead = mmInStream.readInt();
					Log.d("doIn","Entrando al doin..."+lead);
					*/
/*					tam[0]= mmInStream.readByte();
					tam[1]= mmInStream.readByte();
					tam[2]= mmInStream.readByte();
					tam[3]= mmInStream.readByte();			
*/					//if(lead==1){
//					mmInStream.readFully(tam,0,4);
//					mmInStream.read(tam1);

					//Espera hasta que reciba dato del BT
					//String f1= mmInStream.readLine();					
					//Log.d("Tam",""+f1.length());
					int num = Integer.parseInt(f1);
					num += 1;
					if(num == 100)
						num = 0;
					f1 = ""+num;
					//String cad = new String(tam);
					//String cad = mmInStream.readUTF();
					Log.d("Cad1",""+f1);
									//Funcion para llamar onProgressUpdate y mandarle los valores en modo String
					String  []values = new String[2];
					x += 0.015;
					//y = 3;
					values[0] = ""+x;
					values[1] = ""+f1;
					Thread.sleep(25);
					
					publishProgress(values);						
					//Llegados al limite hay que limpiar la pantalla
					//}
					
/*					if(time > frecuencia)
					{
						Log.d("Here:","Time: "+time);
						time = 0;
						time1 = System.currentTimeMillis();
						amplitud *=-1;
					}
*/					//Llegados al limite hay que limpiar la pantalla
					if(x >= xMax)
					{
						restart();
					}
				}
				
			}catch(Exception e){
				Log.w("Wrong","eror: "+e.toString());
			}
			return null;
		}
		
		// Con esta funcion grafica y actualiza los datos
		@Override
		protected void onProgressUpdate(String... values) {
			Log.d("B2","Values: "+values[0]+","+values[1]);
			try
			{
				salida.add(Float.parseFloat(values[0]), Float.parseFloat(values[1]));
				//scalon.add(Float.parseFloat(values[2]), Float.parseFloat(values[3]));
				mChart.repaint();
			}
			catch(Exception e)
			{
				Log.w("Error",e.toString());
				salida.add(0,0);
				//scalon.add(0,0);
				mChart.repaint();				
			}
		}    	
    } 
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	if ((keyCode == KeyEvent.KEYCODE_BACK)) {
		try {
			btSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    Log.d(this.getClass().getName(), "back button pressed");
	}
	return super.onKeyDown(keyCode, event);

	}
	
	public void restart()
	{
    	try
    	{
    		//Thread.sleep(10);
    	}
    	catch(Exception e){}
    	
    	salida.clear();
    	//scalon.clear();

    	//ynext = 0;
		//timecoord = 0;
		//time = 0;
    	x = 0;
		//time1 = System.currentTimeMillis();
	}
}
