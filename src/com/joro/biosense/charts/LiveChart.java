package com.joro.biosense.charts;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.joro.biosense.R;
import com.joro.biosense.ResultsData;

public class LiveChart extends Activity {
	private static final String TAG = "LiveChart";
	public static final String TYPE = "type";

	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

	private XYSeries mCurrentSeries;

	private XYSeriesRenderer mCurrentRenderer;

	private String mDateFormat;

	private GraphicalView mChartView;

	private int index = 0;

	private Cursor mCursor;

	// Results data to access SQLite Database
	private ResultsData dbHelper;

	public static final String C_ID = "_id";
	public static final String C_CREATED_AT = "created_at";
	public static final String C_PULSE = "pulse";
	public static final String C_OXY = "oxy";
	public static final String C_USER = "user";
	public static final String C_UPLOADED = "uploaded";
	
	
	// Broadcast receiver that receives data upload updates
	UpdateGraphReceiver receiver;
	
	//Setting up filter for intents
	IntentFilter filter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Setting up UpdateReceiver broadcast receiver
		receiver = new UpdateGraphReceiver();
		filter = new IntentFilter("com.joro.biosense.NEW_RESULT_INTENT");
		
		setContentView(R.layout.xygraph);
		mRenderer.setApplyBackgroundColor(true);
		mRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
		mRenderer.setAxisTitleTextSize(16);
		mRenderer.setChartTitleTextSize(20);
		mRenderer.setLabelsTextSize(15);
		mRenderer.setLegendTextSize(15);
		mRenderer.setMargins(new int[] { 20, 30, 15, 0 });
		mRenderer.setZoomButtonsVisible(true);
		mRenderer.setPointSize(10);
		
		String seriesTitle = "Series " + (mDataset.getSeriesCount() + 1);
		XYSeries series = new XYSeries(seriesTitle);
		mDataset.addSeries(series);
		mCurrentSeries = series;
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		mRenderer.addSeriesRenderer(renderer);
		renderer.setPointStyle(PointStyle.POINT);
		renderer.setFillPoints(true);
		mCurrentRenderer = renderer;
		Log.i(TAG, "onCreate: Renderer, Dataset, Series set");

		// Open Database helper
		dbHelper = new ResultsData(this);
		dbHelper.open();
		mCursor=dbHelper.getResultsByTime();
		
		
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		dbHelper.close();
		unregisterReceiver(receiver);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		registerReceiver(receiver, filter);
		if (mChartView == null) {
			LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
			mChartView = ChartFactory.getLineChartView(this, mDataset,
					mRenderer);
			mRenderer.setClickEnabled(true);
			mRenderer.setSelectableBuffer(100);
			
			mChartView.setOnClickListener(new View.OnClickListener() {

				public void onClick(View v) {
					SeriesSelection seriesSelection = mChartView
							.getCurrentSeriesAndPoint();
					double[] xy = mChartView.toRealPoint(0);
					if (seriesSelection == null) {
						Toast.makeText(LiveChart.this,
								"No chart element was clicked",
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(
								LiveChart.this,
								"Chart element in series index "
										+ seriesSelection.getSeriesIndex()
										+ " data point index "
										+ seriesSelection.getPointIndex()
										+ " was clicked"
										+ " closest point value X="
										+ seriesSelection.getXValue() + ", Y="
										+ seriesSelection.getValue()
										+ " clicked point value X="
										+ (float) xy[0] + ", Y="
										+ (float) xy[1], Toast.LENGTH_SHORT)
								.show();
					}
				}
			});
			
			mChartView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					SeriesSelection seriesSelection = mChartView
							.getCurrentSeriesAndPoint();
					if (seriesSelection == null) {
						Toast.makeText(LiveChart.this,
								"No chart element was long pressed",
								Toast.LENGTH_SHORT);
						return false; // no chart element was long pressed, so
										// let something
						// else handle the event
					} else {
						Toast.makeText(LiveChart.this,
								"Chart element in series index "
										+ seriesSelection.getSeriesIndex()
										+ " data point index "
										+ seriesSelection.getPointIndex()
										+ " was long pressed",
								Toast.LENGTH_SHORT);
						return true; // the element was long pressed - the event
										// has been
						// handled
					}
					
				}
			});
			
			mChartView.addZoomListener(new ZoomListener() {
				public void zoomApplied(ZoomEvent e) {
					String type = "out";
					if (e.isZoomIn()) {
						type = "in";
					}
					System.out.println("Zoom " + type + " rate "
							+ e.getZoomRate());
				}

				public void zoomReset() {
					System.out.println("Reset");
				}
			}, true, true);
			mChartView.addPanListener(new PanListener() {
				public void panApplied() {
					System.out.println("New X range=["
							+ mRenderer.getXAxisMin() + ", "
							+ mRenderer.getXAxisMax() + "], Y range=["
							+ mRenderer.getYAxisMax() + ", "
							+ mRenderer.getYAxisMax() + "]");
				}
			});
			layout.addView(mChartView, new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		} else {
			mChartView.repaint();
		}
		Log.i(TAG, "onResume5");
		fillChart();
		Log.i(TAG, "onResume6");
		

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		//unregisterReceiver(receiver);
	}

	private void fillChart() {
		//mCursor = dbHelper.getResultsByTime();
		mCursor.moveToNext();
		
		while (!mCursor.isAfterLast()) {
			

			double x = 0;
			double y = 0;
			try {
				x = mCursor.getPosition();
			} catch (NumberFormatException e) {
				// return;
			}
			try {
				y = Double.parseDouble(mCursor.getString(mCursor
						.getColumnIndex(C_PULSE)));
			} catch (NumberFormatException e) {
				// TODO
				// return;
			}
			//Log.i(TAG, "x=" + x + " and y=" + y);
			mCurrentSeries.add(x, y);
			mCursor.moveToNext();
		}

		{
			if (mChartView != null) {
				mChartView.repaint();
				
			}

		}

	}
	

	
	class UpdateGraphReceiver extends BroadcastReceiver { //
		@Override
		public void onReceive(Context context, Intent intent) { //
			RefreshGraph(intent);
			Log.d(TAG, "onReceivedUpdate");
		}
	}
	
	private void RefreshGraph(Intent intent){
		double x;
		double y;
		String pulse=intent.getStringExtra("PULSE");
		Log.d(TAG,"Pulse is: "+pulse);
		mCursor=dbHelper.getResultsByTime();
		mCursor.moveToLast();
		x=mCursor.getPosition();
		y=Double.parseDouble(pulse);
		mCurrentSeries.add(x, y);
		mChartView.repaint();
	}

    

}
