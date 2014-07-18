package org.tsd.tsdbot.functions;

import org.tsd.tsdbot.TSDBot;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Joe on 7/9/2014.
 */
public class Wod implements MainFunction {

    @Override
    public void run(String channel, String sender, String ident, String text) {

        TSDBot bot = TSDBot.getInstance();

        LinkedList<ExerciseImpl> exercises = new LinkedList<>();

        String[] args = text.split("\\s+");
        List<Modifier> mods = Modifier.getModsFromArgs(args);

        // check for illegal mod combinations
        if(mods.contains(Modifier.noPush) && mods.contains(Modifier.noPull)) {
            bot.sendMessage(channel, "arnt u a cheeky cunt m8");
        } else if(mods.contains(Modifier.noUpper) && mods.contains(Modifier.noLower)) {
            bot.sendMessage(channel, "ill punch u in the gabber");
        }

        /**
         * Standard template:
         * A                            B
         * Power                        Lower Ps/Pl Primary
         * Lower        Primary         Upper Push  Primary
         * Upper Push   Primary         Upper Pull  Primary
         * Upper Pull   Secondary       Lower Pl/Ps Secondary
         * Upper Push   Secondary       Upper Ps/Pl Secondary
         * Upper Pull   Tertiary        Upper Pl/Ps Tertiary
         * Upper Push   Tertiary        Abs
         */

        Random rand = new Random();

        if(rand.nextBoolean()) {
            // template A

            if(!mods.contains(Modifier.noPower)) {
                exercises.addLast(new ExerciseImpl(
                        Exercise.getRandomExercise(Type.power, null, Level.power),
                        8,
                        Level.power.getRandomReps())
                );
            }

            if(!mods.contains(Modifier.noLower)) {
                Exercise ex = Exercise.getRandomExercise(Type.lower, null, Level.primary);
                int sets = (ex.equals(Exercise.Deadlift)) ? 1 : 3; // deadlifts are hard
                exercises.addLast(new ExerciseImpl(
                        ex,
                        sets,
                        Level.primary.getRandomReps()
                ));
            }

            if(!mods.contains(Modifier.noUpper)) {

                if(!mods.contains(Modifier.noPush)) {
                    exercises.addLast(new ExerciseImpl(
                            Exercise.getRandomExercise(Type.upper, Direction.push, Level.primary),
                            3,
                            Level.primary.getRandomReps()
                    ));
                }

                if(!mods.contains(Modifier.noPull)) {
                    exercises.addLast(new ExerciseImpl(
                            Exercise.getRandomExercise(Type.upper, Direction.pull, Level.secondary),
                            3,
                            Level.secondary.getRandomReps()
                    ));
                }

                if(!mods.contains(Modifier.noPush)) {
                    exercises.addLast(new ExerciseImpl(
                            Exercise.getRandomExercise(Type.upper, Direction.push, Level.secondary),
                            3,
                            Level.secondary.getRandomReps()
                    ));
                }

                if(!mods.contains(Modifier.noPull)) {
                    exercises.addLast(new ExerciseImpl(
                            Exercise.getRandomExercise(Type.upper, Direction.pull, Level.tertiary),
                            3,
                            Level.tertiary.getRandomReps()
                    ));
                }

                if(!mods.contains(Modifier.noPush)) {
                    exercises.addLast(new ExerciseImpl(
                            Exercise.getRandomExercise(Type.upper, Direction.push, Level.tertiary),
                            3,
                            Level.tertiary.getRandomReps()
                    ));
                }

            }


        } else {
            // template B

            Direction firstDirection = (rand.nextBoolean()) ? Direction.push : Direction.pull;
            Direction secondDirection = (firstDirection.equals(Direction.push)) ? Direction.pull : Direction.push;

            if(!mods.contains(Modifier.noLower)) {
                Exercise ex = Exercise.getRandomExercise(Type.lower, firstDirection, Level.primary);
                int sets = (ex.equals(Exercise.Deadlift)) ? 1 : 3; // deadlifts are hard
                exercises.addLast(new ExerciseImpl(
                        ex,
                        sets,
                        Level.primary.getRandomReps()
                ));
            }

            if(!mods.contains(Modifier.noUpper)) {
                if (!mods.contains(Modifier.noPush)) {
                    exercises.addLast(new ExerciseImpl(
                            Exercise.getRandomExercise(Type.upper, Direction.push, Level.primary),
                            3,
                            Level.primary.getRandomReps()
                    ));
                }

                if (!mods.contains(Modifier.noPull)) {
                    exercises.addLast(new ExerciseImpl(
                            Exercise.getRandomExercise(Type.upper, Direction.pull, Level.primary),
                            3,
                            Level.primary.getRandomReps()
                    ));
                }
            }

            if(!mods.contains(Modifier.noLower)) {
                exercises.addLast(new ExerciseImpl(
                        Exercise.getRandomExercise(Type.lower, secondDirection, Level.secondary),
                        3,
                        Level.secondary.getRandomReps()
                ));
            }

            if(!mods.contains(Modifier.noUpper)) {
                if (!mods.contains(Modifier.noPush)) {
                    exercises.addLast(new ExerciseImpl(
                            Exercise.getRandomExercise(Type.upper, firstDirection, Level.secondary),
                            3,
                            Level.secondary.getRandomReps()
                    ));
                }

                if (!mods.contains(Modifier.noPull)) {
                    exercises.addLast(new ExerciseImpl(
                            Exercise.getRandomExercise(Type.upper, secondDirection, Level.tertiary),
                            3,
                            Level.tertiary.getRandomReps()
                    ));
                }
            }
        }

        bot.sendMessage(channel, "Sending you your personalized workout routine, " + sender + "!");
        for(ExerciseImpl exercise : exercises) {
            bot.sendMessage(sender, exercise.toString());
        }

    }

    public static enum Exercise {
        Barbell_Bench_Press         (Type.upper, Direction.push, Level.primary),
        Incline_Bench_Press         (Type.upper, Direction.push, Level.secondary),
        Close_Grip_Bench_Press      (Type.upper, Direction.push, Level.secondary),
        Dumbbell_Bench_Press        (Type.upper, Direction.push, Level.secondary),
        Dips                        (Type.upper, Direction.push, Level.secondary),
        Dumbbell_Fly                (Type.upper, Direction.push, Level.secondary),
        Lying_Tricep_Extension      (Type.upper, Direction.push, Level.tertiary),
        Tricep_Pushdown             (Type.upper, Direction.push, Level.tertiary),

        Shoulder_Press              (Type.upper, Direction.push, Level.primary),
        Push_Press                  (Type.upper, Direction.push, Level.secondary),
        Dumbbell_Shoulder_Press     (Type.upper, Direction.push, Level.secondary),
        Handstand_Push_Up           (Type.upper, Direction.push, Level.secondary),
        Dumbbell_Lateral_Raise      (Type.upper, Direction.push, Level.tertiary),
        Dumbbell_Front_Raise        (Type.upper, Direction.push, Level.tertiary),
        Overhead_Tricep_Extension   (Type.upper, Direction.push, Level.tertiary),

        Weighted_Pull_Up            (Type.upper, Direction.pull, Level.primary),
        Pendlay_Row                 (Type.upper, Direction.pull, Level.primary),
        One_Arm_Dumbbell_Row        (Type.upper, Direction.pull, Level.secondary),
        Chin_Up                     (Type.upper, Direction.pull, Level.secondary),
        Lat_Pulldown                (Type.upper, Direction.pull, Level.secondary),
        Chest_Supported_Row         (Type.upper, Direction.pull, Level.secondary),
        Barbell_Curl                (Type.upper, Direction.pull, Level.tertiary),
        Incline_Dumbbell_Curl       (Type.upper, Direction.pull, Level.tertiary),

        Power_Shrug                 (Type.upper, Direction.pull, Level.primary),
        Barbell_Shrug               (Type.upper, Direction.pull, Level.secondary),
        Rear_Dumbbell_Fly           (Type.upper, Direction.pull, Level.tertiary),
        Face_Pull                   (Type.upper, Direction.pull, Level.tertiary),

        Squat                       (Type.lower, Direction.push, Level.primary),
        Front_Squat                 (Type.lower, Direction.push, Level.secondary),
        Barbell_Step_Ups            (Type.lower, Direction.push, Level.secondary),
        Barbell_Lunge               (Type.lower, Direction.push, Level.secondary),

        Deadlift                    (Type.lower, Direction.pull, Level.primary),
        Romanian_Deadlift           (Type.lower, Direction.pull, Level.secondary),
        Snatch_Grip_Deadlift        (Type.lower, Direction.pull, Level.secondary),
        Pull_Through                (Type.lower, Direction.pull, Level.secondary),

        Power_Clean                 (Type.power, Direction.pull, Level.power),
        Hang_Power_Clean            (Type.power, Direction.pull, Level.power),
        Clean_Pull                  (Type.power, Direction.pull, Level.secondary),

        Power_Snatch                (Type.power, Direction.pull, Level.power),
        Hang_Power_Snatch           (Type.power, Direction.pull, Level.power),
        Snatch_Pull                 (Type.power, Direction.pull, Level.secondary);

        Type type;
        Direction direction;
        Level level;

        Exercise(Type type, Direction direction, Level level) {
            this.type = type;
            this.direction = direction;
            this.level = level;
        }

        public static Exercise getRandomExercise(Type type, Direction direction, Level level) {
            LinkedList<Exercise> possibilities = new LinkedList<>();
            Random rand = new Random();
            for(Exercise exercise : values()) {
                if(type == null || type.equals(exercise.type)) {
                    if(direction == null || direction.equals(exercise.direction)) {
                        if(level == null || level.equals(exercise.level)) {
                            possibilities.add(exercise);
                        }
                    }
                }
            }
            return possibilities.get(rand.nextInt(possibilities.size()));
        }

    }


    private static enum Level {
        power(1,3),
        primary(3,6),
        secondary(8,12),
        tertiary(15,20);

        int reps_lo;
        int reps_hi;

        Level(int reps_lo, int reps_hi) {
            this.reps_lo = reps_lo;
            this.reps_hi = reps_hi;
        }

        public int getRandomReps() {
            Random random = new Random();
            return random.nextInt(reps_hi - reps_lo) + reps_lo;
        }

    }

    private static enum Direction {
        push,
        pull
    }

    private static enum Type {
        power,
        upper,
        lower
    }

    private static enum Modifier {
        noLower     ("-lower"),
        noUpper     ("-upper"),
        noPush      ("-push"),
        noPull      ("-pull"),
        noPower     ("-power");

        String s;

        Modifier(String s) {
            this.s = s;
        }

        public static List<Modifier> getModsFromArgs(String[] args) {
            List<Modifier> ret = new LinkedList<>();
            for(String arg : args) {
                for(Modifier mod : values()) {
                    if(arg.equalsIgnoreCase(mod.s))
                        ret.add(mod);
                }
            }
            return ret;
        }
    }

    private static class ExerciseImpl {
        public Exercise exercise;
        public int sets;
        public int reps;

        public ExerciseImpl(Exercise exercise, int sets, int reps) {
            this.exercise = exercise;
            this.sets = sets;
            this.reps = reps;
        }

        @Override
        public String toString() {
            return exercise + " " + sets + " sets x " + reps + " reps";
        }
    }
}
