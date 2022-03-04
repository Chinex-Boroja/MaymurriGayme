package com.ihediohachinedu.app.models

enum class BoardSize(val numberOfCards: Int) {
    EASY(8),
    MEDIUM(18),
    HARD(24);

    companion object {
        fun getByValue(value: Int) = values().first(){it.numberOfCards == value}
    }

    fun getWidth(): Int {
        return when (this) {
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }

    fun getHeight(): Int {
        return numberOfCards / getWidth()
    }

    fun getNumberOfPairs(): Int {
        return numberOfCards / 2
    }
}