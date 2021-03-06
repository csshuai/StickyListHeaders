package com.emilsjolander.components.stickylistheaders;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * @author Emil Sjölander
 */
public class StickyListHeadersListView extends ListView implements
		OnScrollListener, OnClickListener {

	public interface OnHeaderClickListener {
		public void onHeaderClick(StickyListHeadersListView l, View header,
				int itemPosition, long headerId, boolean currentlySticky);
	}

	private OnScrollListener scrollListener;
	private boolean areHeadersSticky = true;
	private int dividerHeight;
	private Drawable divider;
	private boolean clippingToPadding;
	private boolean clipToPaddingHasBeenSet;
	private Long currentHeaderId = null;
	private StickyListHeadersAdapterWrapper adapter;
	private OnHeaderClickListener onHeaderClickListener;
	private int headerPosition;
	private ArrayList<View> footerViews;
	private StickyListHeadersListViewWrapper frame;
	private int adapterCount;
	private boolean drawingListUnderStickyHeader = true;

	private DataSetObserver dataSetChangedObserver = new DataSetObserver() {

		@Override
		public void onChanged() {
			adapterCount = adapter.getCount();
			reset();
		}

		@Override
		public void onInvalidated() {
			reset();
		}
	};
	private boolean drawSelectorOnTop;

	public StickyListHeadersListView(Context context) {
		this(context, null);
	}

	public StickyListHeadersListView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.listViewStyle);
	}

	public StickyListHeadersListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);

		super.setOnScrollListener(this);
		// null out divider, dividers are handled by adapter so they look good
		// with headers
		super.setDivider(null);
		super.setDividerHeight(0);
		setVerticalFadingEdgeEnabled(false);
		
		int[] attrsArray = new int[] {
		       android.R.attr.drawSelectorOnTop
		    };
		
		TypedArray a = context.obtainStyledAttributes(attrs,
				attrsArray, defStyle, 0);
		drawSelectorOnTop = a.getBoolean(0, false);
		a.recycle();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (frame == null) {
			ViewGroup parent = ((ViewGroup) getParent());
			int listIndex = parent.indexOfChild(this);
			parent.removeView(this);

			frame = new StickyListHeadersListViewWrapper(getContext());
			frame.setSelector(getSelector());
			frame.setDrawSelectorOnTop(drawSelectorOnTop);
			
			ViewGroup.MarginLayoutParams p = (MarginLayoutParams) getLayoutParams();
			if (clippingToPadding) {
				frame.setPadding(0, getPaddingTop(), 0, getPaddingBottom());
				setPadding(getPaddingLeft(), 0, getPaddingRight(), 0);
			}

			ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
			setLayoutParams(params);
			frame.addView(this);
			frame.setBackgroundDrawable(getBackground());
			super.setBackgroundDrawable(null);

			frame.setLayoutParams(p);
			parent.addView(frame, listIndex);
		}
	}

	@Override
	@Deprecated
	public void setBackgroundDrawable(Drawable background) {
		if (frame != null) {
			frame.setBackgroundDrawable(background);
		}
	}

	@Override
	public void setDrawSelectorOnTop(boolean onTop) {
		super.setDrawSelectorOnTop(onTop);
		drawSelectorOnTop = onTop;
		frame.setDrawSelectorOnTop(drawSelectorOnTop);
	}

	private void reset() {
		if (frame != null) {
			frame.setHeaderBottomPosition(-1);
			frame.removeHeader();
		}
		currentHeaderId = null;
	}

	@Override
	public boolean performItemClick(View view, int position, long id) {
		OnItemClickListener listener = getOnItemClickListener();
		int headerViewsCount = getHeaderViewsCount();
		final int viewType = adapter.getItemViewType(position-headerViewsCount);
		if (viewType == adapter.headerViewType) {
			if (onHeaderClickListener != null) {
				position = adapter.getRealPositionDisregardingHeadersAndDividers(position-headerViewsCount);
				onHeaderClickListener.onHeaderClick(this, view, position, id, false);
				return true;
			}
			return false;
		} else if (viewType == adapter.dividerViewType) {
			return false;
		} else {
			if (listener != null) {
				if(position>=adapterCount){
					position -= adapter.getHeaderCount();
				}else if(!(position<headerViewsCount)){
					position = adapter.getRealPositionDisregardingHeadersAndDividers(position-headerViewsCount) + headerViewsCount;
				}
				listener.onItemClick(this, view, position, id);
				return true;
			}
			return false;
		}
	}

	/**
	 * can only be set to false if headers are sticky, not compatible with
	 * fading edges
	 */
	@Override
	public void setVerticalFadingEdgeEnabled(boolean verticalFadingEdgeEnabled) {
		if (areHeadersSticky) {
			super.setVerticalFadingEdgeEnabled(false);
		} else {
			super.setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled);
		}
	}

	@Override
	public void setDivider(Drawable divider) {
		this.divider = divider;
		if (divider != null) {
			int dividerDrawableHeight = divider.getIntrinsicHeight();
			if (dividerDrawableHeight >= 0) {
				setDividerHeight(dividerDrawableHeight);
			}
		}
		if (adapter != null) {
			adapter.setDivider(divider);
			requestLayout();
			invalidate();
		}
	}

	@Override
	public void setDividerHeight(int height) {
		dividerHeight = height;
		if (adapter != null) {
			adapter.setDividerHeight(height);
			requestLayout();
			invalidate();
		}
	}

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		scrollListener = l;
	}

	public void setAreHeadersSticky(boolean areHeadersSticky) {
		if (this.areHeadersSticky != areHeadersSticky) {
			if (areHeadersSticky) {
				super.setVerticalFadingEdgeEnabled(false);
			}
			requestLayout();
			this.areHeadersSticky = areHeadersSticky;
		}
	}

	public boolean getAreHeadersSticky() {
		return areHeadersSticky;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		if (!clipToPaddingHasBeenSet) {
			clippingToPadding = true;
		}
		if (adapter != null && !(adapter instanceof StickyListHeadersAdapter)) {
			throw new IllegalArgumentException(
					"Adapter must implement StickyListHeadersAdapter");
		}

		if(this.adapter != null){
			this.adapter.unregisterDataSetObserver(dataSetChangedObserver);
			this.adapter = null;
		}

		if(adapter != null){
			this.adapter = new StickyListHeadersAdapterWrapper(getContext(),
					(StickyListHeadersAdapter) adapter);
			this.adapter.setDivider(divider);
			this.adapter.setDividerHeight(dividerHeight);
			this.adapter.registerDataSetObserver(dataSetChangedObserver);
			adapterCount = this.adapter.getCount();
		}
		
		reset();
		super.setAdapter(this.adapter);
	}

	public StickyListHeadersAdapter getWrappedAdapter() {
		if (adapter != null) {
			return adapter.getDelegate();
		}
		return null;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			post(new Runnable() {
				
				@Override
				public void run() {
					scrollChanged(getFirstVisiblePosition());
				}
			});
		}
		if(!drawingListUnderStickyHeader){
			canvas.clipRect(0, Math.max(frame.getHeaderBottomPosition(), 0), canvas.getWidth(), canvas.getHeight());
		}
		super.dispatchDraw(canvas);
	}

	@Override
	public void setClipToPadding(boolean clipToPadding) {
		super.setClipToPadding(clipToPadding);
		clippingToPadding = clipToPadding;
		clipToPaddingHasBeenSet = true;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (scrollListener != null) {
			scrollListener.onScroll(view, firstVisibleItem, visibleItemCount,
					totalItemCount);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			scrollChanged(firstVisibleItem);
		}
	}

	private void scrollChanged(int firstVisibleItem) {
		if (adapter == null) {
			return;
		}

		if (adapterCount == 0 || !areHeadersSticky) {
			return;
		}

		final int listViewHeaderCount = getHeaderViewsCount();
		firstVisibleItem = getFixedFirstVisibleItem(firstVisibleItem)
				- listViewHeaderCount;

		if (firstVisibleItem < 0 || firstVisibleItem > adapterCount - 1) {
			if(currentHeaderId != null){
				reset();
				updateHeaderVisibilities();
				invalidate();
			}
			return;
		}

		boolean headerHasChanged = false;
		long newHeaderId = adapter.getHeaderId(firstVisibleItem);
		if (currentHeaderId == null || currentHeaderId != newHeaderId) {
			headerPosition = firstVisibleItem;
			View header = adapter.getHeaderView(headerPosition,
					frame.removeHeader(), frame);
			header.setOnClickListener(this);
			frame.setHeader(header);
			headerHasChanged = true;
		}
		currentHeaderId = newHeaderId;
		
		int childCount = getChildCount();

		if (childCount > 0) {
			View viewToWatch = null;
			int watchingChildDistance = Integer.MAX_VALUE;
			boolean viewToWatchIsFooter = false;

			for (int i = 0; i < childCount; i++) {
				View child = getChildAt(i);
				boolean childIsFooter = footerViews != null
						&& footerViews.contains(child);

				int childDistance;
				if (clippingToPadding) {
					childDistance = child.getTop() - getPaddingTop();
				} else {
					childDistance = child.getTop();
				}

				if (childDistance < 0) {
					continue;
				}

				if (viewToWatch == null
						|| (!viewToWatchIsFooter && !adapter
								.isHeader(viewToWatch))
						|| ((childIsFooter || adapter.isHeader(child)) && childDistance < watchingChildDistance)) {
					viewToWatch = child;
					viewToWatchIsFooter = childIsFooter;
					watchingChildDistance = childDistance;
				}
			}

			int headerHeight = frame.getHeaderHeight();
			int headerBottomPosition = 0;
			if (viewToWatch != null
					&& (viewToWatchIsFooter || adapter.isHeader(viewToWatch))) {

				if (firstVisibleItem == listViewHeaderCount
						&& getChildAt(0).getTop() > 0 && !clippingToPadding) {
					headerBottomPosition = 0;
				} else {
					if (clippingToPadding) {
						headerBottomPosition = Math.min(viewToWatch.getTop(),
								headerHeight + getPaddingTop());
						headerBottomPosition = headerBottomPosition < getPaddingTop() ? headerHeight
								+ getPaddingTop()
								: headerBottomPosition;
					} else {
						headerBottomPosition = Math.min(viewToWatch.getTop(),
								headerHeight);
						headerBottomPosition = headerBottomPosition < 0 ? headerHeight
								: headerBottomPosition;
					}
				}
			} else {
				headerBottomPosition = headerHeight;
				if (clippingToPadding) {
					headerBottomPosition += getPaddingTop();
				}
			}
			if(frame.getHeaderBottomPosition() != headerBottomPosition || headerHasChanged){
				frame.setHeaderBottomPosition(headerBottomPosition);
			}
			updateHeaderVisibilities();
		}
	}
	
	@Override
	public void setSelector(Drawable sel) {
		super.setSelector(sel);
		if(frame != null){
			frame.setSelector(sel);
		}
	}

	@Override
	public void addFooterView(View v) {
		super.addFooterView(v);
		if (footerViews == null) {
			footerViews = new ArrayList<View>();
		}
		footerViews.add(v);
	}

	@Override
	public boolean removeFooterView(View v) {
		boolean removed = super.removeFooterView(v);
		if (removed) {
			footerViews.remove(v);
		}
		return removed;
	}

	private void updateHeaderVisibilities() {
		int top = clippingToPadding ? getPaddingTop() : 0;
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			View child = getChildAt(i);
			if (adapter.isHeader(child)) {
				if (child.getTop() < top) {
					if(child.getVisibility() != View.INVISIBLE){
						child.setVisibility(View.INVISIBLE);
					}
				} else {
					if(child.getVisibility() != View.VISIBLE){
						child.setVisibility(View.VISIBLE);
					}
				}
			}
		}
	}

	private int getFixedFirstVisibleItem(int firstVisibleItem) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return firstVisibleItem;
		}

		for (int i = 0; i < getChildCount(); i++) {
			if (getChildAt(i).getBottom() >= 0) {
				firstVisibleItem += i;
				break;
			}
		}

		// work around to fix bug with firstVisibleItem being to high because
		// listview does not take clipToPadding=false into account
		if (!clippingToPadding && getPaddingTop() > 0) {
			if (super.getChildAt(0).getTop() > 0) {
				if (firstVisibleItem > 0) {
					firstVisibleItem -= 1;
				}
			}
		}
		return firstVisibleItem;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollListener != null) {
			scrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	@Override
	public void setSelectionFromTop(int position, int y) {
		if (areHeadersSticky) {
			if (frame != null && frame.hasHeader()) {
				y += frame.getHeaderHeight();
			}
		}
		super.setSelectionFromTop(position, y);
	}

	public void setOnHeaderClickListener(
			OnHeaderClickListener onHeaderClickListener) {
		this.onHeaderClickListener = onHeaderClickListener;
	}

	@Override
	public void onClick(View v) {
		if (frame.isHeader(v)) {
			if (onHeaderClickListener != null) {
				onHeaderClickListener.onHeaderClick(this, v, headerPosition,
						currentHeaderId, true);
			}
		}
	}

	public boolean isDrawingListUnderStickyHeader() {
		return drawingListUnderStickyHeader;
	}

	public void setDrawingListUnderStickyHeader(boolean drawingListUnderStickyHeader) {
		this.drawingListUnderStickyHeader = drawingListUnderStickyHeader;
	}

}
