package com.example.drawer2

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.nio.charset.Charset

/**
 * Fragmentti, joka hallitsee päiväkirjan toimintoja: listaus, lukeminen, kirjoittaminen ja suojaus.
 */
class PaivakirjaFragment : Fragment(R.layout.fragment_paivakirja) {

    private var layoutList: LinearLayout? = null
    private var layoutEditor: LinearLayout? = null
    private var containerNotesList: LinearLayout? = null
    private var etNote: EditText? = null
    private var btnDeleteSelected: Button? = null
    
    private val folderName = "diary_notes"
    private var currentEditingFile: File? = null
    private val selectedFiles = mutableSetOf<File>()
    private var isUnlocked = false

    //hakee kaikki ui elementit käyttöön, kytkee toiminnallisuudet buttoneihin ja päättää
    //näytetäänkö salasanan kysyminen vai lista merkinnöistä
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutList = view.findViewById(R.id.layoutList)
        layoutEditor = view.findViewById(R.id.layoutEditor)
        containerNotesList = view.findViewById(R.id.containerNotesList)
        etNote = view.findViewById(R.id.etNote)
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected)

        view.findViewById<Button>(R.id.btnNewNote)?.setOnClickListener { showEditor(null) }
        view.findViewById<Button>(R.id.btnBackToList)?.setOnClickListener { showList() }
        view.findViewById<Button>(R.id.btnSave)?.setOnClickListener { saveNote() }
        btnDeleteSelected?.setOnClickListener { deleteSelectedNotes() }

        if (!isUnlocked) checkPassword() else showList()
    } //näyttää listan jos ei ole lukittu muuten ei


    //salasanan tarkistusta jne
    private fun checkPassword() {
        val context = context ?: return
        //haetaan tallennettu salasana muistista
        val prefs = context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
        val savedPassword = prefs.getString("password", null)
        
        //ladataan se pyöreä dialogi tähän
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_password, null)
        val etPassword = dialogView.findViewById<EditText>(R.id.etDialogPassword)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val btnOk = dialogView.findViewById<Button>(R.id.btnDialogOk)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCancel)
        
        //jos salasanaa ei oo vielä olemassa niin pyydetään tekemään semmonen
        if (savedPassword == null) {
            tvTitle.text = "Aseta uusi salasana"
            etPassword.hint = "Luo salasana"
        } else {
            //muuten kysytään se vanha salasana
            tvTitle.text = "Anna salasana"
            etPassword.hint = "Kirjoita salasana"
        }
        
        //rakennetaan se popup ikkuna
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        //pistetään tausta läpinäkyväksi että ne pyöreät kulmat ja borderit näkyy oikein
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        //mitä käy kun ok nappia painetaan
        btnOk.setOnClickListener {
            val pass = etPassword.text.toString()
            if (savedPassword == null) {
                //jos eka kerta niin tallennetaan uusi salasana
                if (pass.isNotEmpty()) {
                    prefs.edit().putString("password", pass).apply()
                    isUnlocked = true
                    dialog.dismiss()
                    showList()
                } else {
                    Toast.makeText(context, "Aseta salasana!", Toast.LENGTH_SHORT).show()
                }
            } else {
                //jos salasana oli jo niin katsotaan oliko se oikein
                if (pass == savedPassword) {
                    isUnlocked = true
                    dialog.dismiss()
                    showList()
                } else {
                    Toast.makeText(context, "Väärä salasana!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //jos painetaan takaisin niin suljetaan tää ja heitetään käyttäjä takas ostoksiin
        btnCancel.setOnClickListener {
            dialog.dismiss()
            (activity as? MainActivity)?.updateSelection(R.id.nav_home, "Ostokset")
        }
    }

    //näyttää merkinnät
    private fun showList() {
        layoutList?.visibility = View.VISIBLE
        layoutEditor?.visibility = View.GONE
        currentEditingFile = null
        selectedFiles.clear()
        updateDeleteButtonVisibility()
        refreshNotesList()
    }

    private fun showEditor(file: File?) {
        layoutList?.visibility = View.GONE
        layoutEditor?.visibility = View.VISIBLE
        currentEditingFile = file
        
        if (file != null) {
            val lines = file.readLines(Charsets.UTF_8)
            if (lines.isNotEmpty()) {
                val content = lines.drop(1).joinToString("\n")
                etNote?.setText(content)
            }
        } else {
            etNote?.setText("")
        }
    }

    private fun refreshNotesList() {
        val container = containerNotesList ?: return
        container.removeAllViews()
        val files = notesDir().listFiles { f -> f.isFile && f.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
        
        if (files.isEmpty()) {
            container.addView(TextView(context).apply { 
                text = "Ei merkintöjä."
                gravity = Gravity.CENTER
                setPadding(0, 50, 0, 0) 
            })
            return
        }

        val inflater = LayoutInflater.from(context)
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        for (file in files) {
            val itemView = inflater.inflate(R.layout.item_note, container, false)
            val tvName = itemView.findViewById<TextView>(R.id.tvNoteName)
            val tvDate = itemView.findViewById<TextView>(R.id.tvNoteDate)
            val cbSelect = itemView.findViewById<CheckBox>(R.id.cbSelect)
            
            val firstLine = file.bufferedReader(Charsets.UTF_8).use { it.readLine() } ?: "Nimetön"
            tvName.text = firstLine
            tvDate.text = dateFormat.format(Date(file.lastModified()))

            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedFiles.add(file) else selectedFiles.remove(file)
                updateDeleteButtonVisibility()
            }

            itemView.setOnClickListener { showEditor(file) }
            container.addView(itemView)
        }
    }
    //poistonapin päivitys näkyvyydestä
    private fun updateDeleteButtonVisibility() { 
        btnDeleteSelected?.visibility = if (selectedFiles.isNotEmpty()) View.VISIBLE else View.GONE 
    }

    //poistetaan valitut
    private fun deleteSelectedNotes() {
        val context = context ?: return
        AlertDialog.Builder(context).setTitle("Poista merkinnät").setMessage("Poistetaanko valitut?")
            .setPositiveButton("Poista") { _, _ -> 
                selectedFiles.forEach { it.delete() }
                showList() 
            }.setNegativeButton("Peruuta", null).show()
    }

    private fun notesDir(): File { 
        val dir = File(requireContext().filesDir, folderName)
        if (!dir.exists()) dir.mkdirs()
        return dir 
    }

    //merkinnän tallennus
    private fun saveNote() {
        val context = context ?: return
        val text = etNote?.text?.toString() ?: ""
        
        if (currentEditingFile != null) {
            val firstLine = currentEditingFile!!.bufferedReader(Charsets.UTF_8).use { it.readLine() } ?: "Nimetön"
            currentEditingFile!!.writeText("$firstLine\n$text", Charsets.UTF_8)
            Toast.makeText(context, "Päivitetty!", Toast.LENGTH_SHORT).show()
            showList()
            return
        }

        val input = EditText(context).apply { 
            hint = "Otsikko"
            gravity = Gravity.CENTER 
        }
        AlertDialog.Builder(context).setTitle("Tallenna merkintä").setView(input).setPositiveButton("Tallenna") { _, _ ->
            val title = input.text.toString().trim()
            if (title.isNotEmpty()) {
                val fileName = "note_${System.currentTimeMillis()}.txt"
                val file = File(notesDir(), fileName)
                file.writeText("$title\n$text", Charsets.UTF_8)
                showList()
            } else {
                Toast.makeText(context, "Anna otsikko!", Toast.LENGTH_SHORT).show()
            }
        }.setNegativeButton("Peruuta", null).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        layoutList = null
        layoutEditor = null
        containerNotesList = null
        etNote = null
        btnDeleteSelected = null
    }
}