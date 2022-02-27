package com.ihediohachinedu.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ihediohachinedu.app.models.BoardSize
import com.ihediohachinedu.app.utils.DEFAULT_ICONS

class MainActivity : AppCompatActivity() {

    //private member variables
    private lateinit var boardDisplay: RecyclerView
    private lateinit var numberOfMoves: TextView
    private lateinit var numberOfPairs: TextView

    private var boardSize: BoardSize = BoardSize.HARD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        boardDisplay = findViewById(R.id.rvBoard)
        numberOfMoves = findViewById(R.id.numMoves)
        numberOfPairs = findViewById(R.id.numPairs)

        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumberOfPairs())
        val randomizedImages = (chosenImages + chosenImages).shuffled()


        boardDisplay.adapter = MemoryBoardAdapter(this, boardSize, randomizedImages)
        boardDisplay.setHasFixedSize(false)
        boardDisplay.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }
}