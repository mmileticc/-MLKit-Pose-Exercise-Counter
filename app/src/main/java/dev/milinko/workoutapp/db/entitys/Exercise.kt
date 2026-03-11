package dev.milinko.workoutapp.db.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class Exercise (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name : String  = "",
    val type : Boolean = true, //numerable or timeable (counting reps or time in seconds
    val numOf: Int = 0, //if numerable then reps else seconds
//    val numOfSeconds: Int? = null,
    val date: Date = Date()

)
