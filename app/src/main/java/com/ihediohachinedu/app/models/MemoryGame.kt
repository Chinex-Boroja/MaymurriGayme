package com.ihediohachinedu.app.models

import com.ihediohachinedu.app.utils.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize, private val customGameImages: List<String>?) {

    val images: List<MemoryCard>
    var numberOfPairsFound = 0
    private var numberOfImageFlip = 0

    private var indexOfSingleSelectedImage: Int? = null

    init {
        if (customGameImages == null) {
            val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumberOfPairs())
            val randomizedImages = (chosenImages + chosenImages).shuffled()
            images = randomizedImages.map { MemoryCard(it) }
        } else {
            val randomizedImages = (customGameImages + customGameImages).shuffled()
            //taking an image url and converting to a unique integer
            images = randomizedImages.map { MemoryCard(it.hashCode(), it) }
        }
    }

    fun flipImage(position: Int): Boolean{
        numberOfImageFlip++
        val image = images[position]
        //Three cases
        //0 cards previously flipped over => restore cards + flip over the selected
        //1 card previously flipped over => flip over the selected card + check if the images match
        //2 cards previously flipped over => restore cards + flip over the selected card
        var foundMatch = false

        if (indexOfSingleSelectedImage == null) {
            // 0 or 2 cards previously flipped over
            restoreImages()
            indexOfSingleSelectedImage = position
        } else {
            foundMatch = checkForMatch(indexOfSingleSelectedImage!!, position)
            indexOfSingleSelectedImage = null

        }
        image.isFaceUp = !image.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if (images[position1].identifier != images[position2].identifier) {
            return false
        }
        images[position1].isMatched = true
        images[position2].isMatched = true
        numberOfPairsFound++
        return true
    }
    private fun restoreImages() {
         for (image in images) {
             if (!image.isMatched) {
                 image.isFaceUp = false
             }
         }
    }

    fun haveWonGame(): Boolean {
        return numberOfPairsFound == boardSize.getNumberOfPairs()
    }
    fun isImageFaceUp(position: Int): Boolean {
        return images[position].isFaceUp
    }
    fun getNumberOfMoves(): Int {
        return numberOfImageFlip / 2
    }
}