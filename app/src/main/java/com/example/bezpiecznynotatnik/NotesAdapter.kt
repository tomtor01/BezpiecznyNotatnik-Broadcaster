package com.example.bezpiecznynotatnik

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(
    private val decryptedNotes: List<String>, // Decrypted message list
    private val originalNotes: List<Note>,   // Original Note objects
    private val onEditNote: (Note) -> Unit,
    private val onDeleteNote: (Note) -> Unit ) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_view, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = originalNotes[position]
        holder.bind(decryptedNotes[position])

        holder.itemView.setOnClickListener {
            onEditNote(note)
        }

        holder.buttonDelete.setOnClickListener {
            onDeleteNote(note)
        }
    }

    override fun getItemCount() = decryptedNotes.size

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textNote: TextView = itemView.findViewById(R.id.noteContent)
        val buttonDelete: Button = itemView.findViewById(R.id.buttonDeleteNote)

        fun bind(noteContent: String) {
            textNote.text = noteContent
        }
    }
}
