package dev.milinko.workoutapp.hilt

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.milinko.workoutapp.db.ExerciseDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context) =
        ExerciseDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideExerciseDao(database: ExerciseDatabase) =
        database.exerciseDao()

}