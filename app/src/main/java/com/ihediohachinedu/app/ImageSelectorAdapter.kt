package com.ihediohachinedu.app

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ihediohachinedu.app.models.BoardSize
import kotlin.math.min

class ImageSelectorAdapter(
    private val context: Context,
    private val selectedImageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImageSelectorAdapter.ViewHolder>() {

    interface ImageClickListener {
        fun onPlaceholderClicked()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.game_image, parent, false)
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.getHeight()
        val cardSideLength = min(cardWidth, cardHeight)
        val layoutParams = view.findViewById<ImageView>(R.id.customIV).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
         if (position < selectedImageUris.size) {
             holder.bind(selectedImageUris[position])
         }else {
             holder.bind()
         }
    }

    override fun getItemCount() = boardSize.getNumberOfPairs()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val customImageIV = itemView.findViewById<ImageView>(R.id.customIV)

        fun bind(uri: Uri) {
            customImageIV.setImageURI(uri)
            customImageIV.setOnClickListener(null)
        }

        fun bind() {
            customImageIV.setOnClickListener {
                //launch the intent for user to select photos
                 imageClickListener.onPlaceholderClicked()
            }
        }
    }
}
