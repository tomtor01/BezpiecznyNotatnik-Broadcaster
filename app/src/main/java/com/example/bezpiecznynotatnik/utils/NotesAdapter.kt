package com.example.bezpiecznynotatnik.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bezpiecznynotatnik.Note
import com.example.bezpiecznynotatnik.R

class NotesAdapter(
    private val decryptedNotes: List<String>,
    private val originalNotes: List<Note>,
    private val onEditNote: (Note) -> Unit ) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = originalNotes[position]
        holder.bind(decryptedNotes[position])

        holder.itemView.setOnClickListener {
            onEditNote(note)
        }
    }
    override fun getItemCount() = decryptedNotes.size

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textNote: TextView = itemView.findViewById(R.id.noteContent)

        fun bind(noteContent: String) {
            textNote.text = noteContent
        }
    }
}
