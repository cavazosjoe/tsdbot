package org.tsd.tsdbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Joe on 2/19/14.
 */
public class TomCruise {

    private static final String[] quotes = new String[] {
            "Get with it. Millions of galaxies of hundreds of millions of stars, in a speck on one in a blink. That’s " +
                    "us, lost in space. The cop, you, me… Who notices? (Collateral)",
            "You want me to kill Jappos, I'll kill Jappos. You want me to kill THE ENEMIES of Jappos, I'll kill " +
                    "THE ENEMIES of Jappos... Rebs, or Sioux, or Cheyenne... For 500 bucks a month I'll kill whoever " +
                    "you want. But keep one thing in mind: I'd happily kill you for free. (The Last Samurai)",
            "THATSRIGHT, Ice... man. I am dangerous. *chomp* (Top Gun)",
            "Even in my dreams, I’m an idiot who knows he’s about to wake up to reality. (Vanilla Sky)",
            "Show me the money! (Jerry Maguire)",
            "I am out here for you. You don’t know what it’s like to be ME out here for YOU. It is an up-at-dawn, " +
                    "pride-swallowing siege that I will never fully tell you about, ok? (Jerry Maguire)",
            "That’s more than a dress. That’s an Audrey Hepburn movie. (Jerry Maguire)",
            "I will not rest until I have you holding a Coke, wearing your own shoe, playing a Sega game featuring you, " +
                    "while singing your own song in a new commercial, starring you, broadcast during the Superbowl, in " +
                    "a game that you are winning, and I will not sleep until that happens. I’ll give you fifteen minutes" +
                    " to call me back. (Jerry Maguire)",
            "Twenty-four hours ago, man, I was hot! Now… I’m a cautionary tale. You see this jacket I’m wearing, you " +
                    "like it? Because I don’t really need it. Because I’m cloaked in failure! I lost the number one " +
                    "draft picked the night before the draft! Why? Let’s recap: Because a hockey player’s kid made me " +
                    "feel like a superficial jerk. I ate two slices of bad pizza, went to bed and grew a conscience! " +
                    "(Jerry Maguire)",
            "You... complete me. (Jerry Maguire)",
            "Evildoers are easier, and they taste better. (Interview With The Vampire)",
            "I'm sorry ma’am, I lied to you. I’m very sorry about that. That man right there is my brother and if he " +
                    "doesn’t get to watch ’People’s Court’ in about 30 seconds, he’s gonna throw a fit right here on your " +
                    "porch. Now you can help me or you can stand there and watch it happen. (Rain Man)",
            "I feel the need -- the need for speed. Ow! (Top Gun)",
            "You don't have time to think up there. If you think, you're dead. (Top Gun)",
            "This is what I call a target-rich environment. (Top Gun)",
            "It seems to me that if there were any logic to our language, trust would be a four letter word. (Risky Business)",
            "Porsche. There is no substitute. (Risky Business)",
            "I've got a trig midterm tomorrow and I'm being chased by Guido the Killer Pimp. (Risky Business)",
            "All right, tell me what's going on, tell me what's going on. I'm BLIND, goddammit! (Day of Thunder)",
            "We just rolled up a snowball and threw it into Hell. Now we'll see if it has a chance. (Mission: Impossible 2)",
            "I am the last barman poet / I see America drinking the fabulous cocktails I make / Americans getting " +
                    "stinky on something I stir or shake. (Cocktail)",
            "Talk to me, Goose. (Top Gun)",
            "That son of a bitch cut me off! (Top Gun)",
            "Any of you boys seen an aircraft carrier around here? (Top Gun)",
            "Jesus Christ, and you think I'm reckless? When I fly, I'll have you know that my crew and my plane come first! (Top Gun)",
            "Just want to serve my country, be the best pilot in the Navy, sir. (Top Gun)",
            "No, actually I had this counter in mind. (Top Gun)",
            "Too close for missiles, I'm switching to guns. (Top Gun)",
            "I can see it's dangerous for you, but if the government trusts me, maybe you could. *smile* (Top Gun)",
            "There was once a battle at a place called Thermopylae, where three hundred brave Greeks held off a " +
                    "Persian army of a million men... a million, you understand this number? (The Last Samurai)",
            "Spring, 1877. This marks the longest I've stayed in one place since I left the farm at 17. There is so " +
                    "much here I will never understand. I've never been a church going man, and what I've seen on the " +
                    "field of battle has led me to question God's purpose. But there is indeed something spiritual in " +
                    "this place. And though it may forever be obscure to me, I cannot but be aware of its power. I do " +
                    "know that it is here that I've known my first untroubled sleep in many years. (The Last Samurai)",
            "You have no idea what I have done. (The Last Samurai)",
            "I know why you don't talk. Because you're angry. You're angry because they make you wear a dress. (The Last Samurai)",
            "Okay, look, here's the deal. Man, you were gonna drive me around tonight, never be the wiser, but El " +
                    "Gordo got in front of a window, did his high dive, we're into Plan B. Still breathing? Now we " +
                    "gotta make the best of it, improvise, adapt to the environment, Darwin, shit happens, I Ching, " +
                    "whatever man, we gotta roll with it. (Collateral)",
            "Max, six billion people on the planet, you're getting bent out of shape cause of one fat guy. (Collateral)",
            "Someday? Someday my dream will come? One night you will wake up and discover it never happened. It's " +
                    "all turned around on you. It never will. Suddenly you are old. Didn't happen, and it never will, " +
                    "because you were never going to do it anyway. You'll push it into memory and then zone out in " +
                    "your barco lounger, being hypnotized by daytime TV for the rest of your life. Don't you talk to " +
                    "me about murder. All it ever took was a down payment on a Lincoln town car. That girl, you " +
                    "can't even call that girl. What the fuck are you still doing driving a cab? (Collateral)"
    };

    private static final String[] clips = new String[] {
            "https://www.youtube.com/watch?v=7yP9MmzyTIg",
            "https://www.youtube.com/watch?v=LMT1r9IpIf0",
            "https://www.youtube.com/watch?v=B9XyCwmh-5Q",
            "https://www.youtube.com/watch?v=QE3yMEfpk6E",
            "https://www.youtube.com/watch?v=GhT2XYQ5Yj0",
            "https://www.youtube.com/watch?v=rZ855676Hvo",
            "https://www.youtube.com/watch?v=ISfFg4zHImQ",
            "https://www.youtube.com/watch?v=IejpzbxKUZE",
            "https://www.youtube.com/watch?v=nf7_eayBkxA",
            "https://www.youtube.com/watch?v=XO4f-JqXG7g"
    };

    public static String getRandom() {
        ArrayList<String> bigList = new ArrayList<>(Arrays.asList(quotes));
        bigList.addAll(Arrays.asList(clips));
        Collections.shuffle(bigList);
        return bigList.get(0);
    }

    public static String getRandomClip() {
        return clips[(int)(Math.random() * clips.length)];
    }

    public static String getRandomQuote() {
        return clips[(int)(Math.random() * quotes.length)];
    }
}
