package com.example.kidsdrawing

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

class DrawingView(context: Context, attr: AttributeSet): View(context, attr) {

    private var mDrawingPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mCanvas: Canvas? = null
    private var mBrushSize = 0.toFloat()
    private var mColor = Color.RED
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()


    init {

        setUpDrawing()
    }

    public fun undoDrawing(){
        if (mPaths.size > 0){

            /**
             * hm aub is ka last index wala element utha kar us ko new list men add kr ray hen
             * aur old list se usy remove kar ray hen
             */

            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate() // jesy hi hum invalidate krty hen hamara onDraw wala method chal parta hy..
        }
    }

    public fun redoDrawing(){
        if (mUndoPaths.size > 0){

            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size - 1))
            invalidate()
        }
    }

   private fun setUpDrawing(){

        mDrawingPath = CustomPath(mColor, mBrushSize)
        mDrawPaint = Paint()
        mDrawPaint!!.color = mColor
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        mBrushSize = 7.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawBitmap(mCanvasBitmap!!, 0f, 0f, mDrawPaint)

        for (path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas?.drawPath(path, mDrawPaint!!)
        }

        //yani agar yeh not empty ho k is par kuch draw kiya ja ra ho tabhi path set ho
        if (!mDrawingPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawingPath!!.brushThickness
            mDrawPaint!!.color = mDrawingPath!!.color
            canvas?.drawPath(mDrawingPath!!, mDrawPaint!!)
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){

            // is ka matlab k jesy screen par finger rakhi touch kiya
            MotionEvent.ACTION_DOWN -> {

                //aub yahan hum apny draw hony waly path ki brush ka size aur color select kar ray hen
                mDrawingPath!!.brushThickness = mBrushSize
                mDrawingPath!!.color = mColor
                //yani drawing path ko reset kar ray hen agar koi pehly se hy tu us ko clear kar ray hen screen se
                mDrawingPath!!.reset()
                mDrawingPath!!.moveTo(touchX!!, touchY!!)
            }

            // is ka matlab k jesy finger move karny lagy
            MotionEvent.ACTION_MOVE -> {

                mDrawingPath!!.lineTo(touchX!!, touchY!!)
            }

            // is ka matlab k jesy finger uthai move karny k bad
            MotionEvent.ACTION_UP -> {

                mPaths.add((mDrawingPath!!))
                mDrawingPath = CustomPath(mColor, mBrushSize)
            }
            else -> {
                return false
            }
        }

        //yeh lazmi likhna hy bass is k bagair nhi hoga
        invalidate()
        return true
    }

    public fun setBurshSize(newSize: Float){

        /**
         * yeh jo maine TypedValue k andar apna new size dya hy
         * is ka matlab hy k yeh size mera
         * screen to screen dimension k hisaab se vary kary ga
         * agar bari screen hogi like tablet screen tu us py aur hoga
         * aur choti py aur
         */
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize,
            resources.displayMetrics)
    }

    public fun setColor(newColor: String) {
        mColor = Color.parseColor(newColor)
        mDrawPaint?.color = mColor
    }

    /**
     * yeh class sab se pehly bnaty hen khud hi
     * isi class se hi hum sab kuch bna ray hen
     */
    internal inner class CustomPath(var color: Int, var brushThickness: Float): Path() {


    }
}