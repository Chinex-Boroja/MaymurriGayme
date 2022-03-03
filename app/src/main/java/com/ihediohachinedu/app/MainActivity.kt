package com.ihediohachinedu.app

import android.animation.ArgbEvaluator
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.ihediohachinedu.app.models.BoardSize
import com.ihediohachinedu.app.models.MemoryCard
import com.ihediohachinedu.app.models.MemoryGame
import com.ihediohachinedu.app.newgame.CreateNewGame
import com.ihediohachinedu.app.utils.DEFAULT_ICONS
import com.ihediohachinedu.app.utils.EXTRA_BOARD_SIZE

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 234
    }

    //private member variables
    private lateinit var boardDisplay: RecyclerView
    private lateinit var numberOfMoves: TextView
    private lateinit var numberOfPairs: TextView
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter

    private lateinit var layoutRoot: ConstraintLayout

    private var boardSize: BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        boardDisplay = findViewById(R.id.rvBoard)
        numberOfMoves = findViewById(R.id.numMoves)
        numberOfPairs = findViewById(R.id.numPairs)

        layoutRoot = findViewById(R.id.layoutRoot)

        setupGame()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_refresh -> {
                if (memoryGame.getNumberOfMoves() > 0  && !memoryGame.haveWonGame()) {
                    displayAlertDialog("Quit your current game?", null, View.OnClickListener {
                        setupGame()
                    })
                }else{
                    //setup the game again
                    setupGame()
                }
                return true
            }
            R.id.new_size -> {
                displayNewSizeDialog()
                return true
            }
            R.id.menu_item_custom -> {
                displayCreationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroupSize)

        displayAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
            //Set a new value for the board size of the game
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rdEasy -> BoardSize.EASY
                R.id.rdMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //Navigate to a new activity
            val intent = Intent(this, CreateNewGame::class.java)
            //passing the data for request to do option
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })
    }

    private fun displayNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroupSize)

        //default boardSize when the application loads
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rdEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rdMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rdHard)
        }
        displayAlertDialog("Choose new size", boardSizeView, View.OnClickListener {
            //Set a new value for the board size of the game
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rdEasy -> BoardSize.EASY
                R.id.rdMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //reload the game
            setupGame()
        })
    }

    private fun displayAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title )
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") {_,_ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun setupGame() {
        when (boardSize) {
            BoardSize.EASY -> {
                numberOfMoves.text = "Easy: 4 x 2"
                numberOfPairs.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                numberOfMoves.text = "Medium: 6 x 3"
                numberOfPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                numberOfMoves.text = "Hard: 6 x 4"
                numberOfPairs.text = "Pairs: 0 / 12"
            }
        }
        numberOfPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        //construct a list of memory game
        memoryGame = MemoryGame(boardSize)
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.images, object: MemoryBoardAdapter.ImageClickListener {
            override fun onImageClicked(position: Int) {
                updateGameWithFlip(position)
            }
        } )
        boardDisplay.adapter = adapter
        boardDisplay.setHasFixedSize(false)
        boardDisplay.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }


    private fun updateGameWithFlip(position: Int) {
        //Error checking
        if (memoryGame.haveWonGame()) {
            //Alert the user of an invalid move
                Snackbar.make(layoutRoot, "You already won!", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (memoryGame.isImageFaceUp(position)) {
            //Alert the user of an invalid move
            Snackbar.make(layoutRoot, "Invalid move!", Snackbar.LENGTH_SHORT).show()
            return
        }
        //Actual flip
        if (memoryGame.flipImage(position)) {
            Log.i(TAG, "Found a match!, Num pairs found: ${memoryGame.numberOfPairsFound}")
            val color = ArgbEvaluator().evaluate(
                memoryGame.numberOfPairsFound.toFloat() / boardSize.getNumberOfPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full)
            ) as Int
            numberOfPairs.setTextColor(color)
            numberOfPairs.text = "Pairs: ${memoryGame.numberOfPairsFound} / ${boardSize.getNumberOfPairs()}"
            if (memoryGame.haveWonGame()) {
                Snackbar.make(layoutRoot, "You won! Congratulations.", Snackbar.LENGTH_LONG).show()
            }
        }
        numberOfMoves.text = "Moves: ${memoryGame.getNumberOfMoves()}"
        adapter.notifyDataSetChanged()
    }
}