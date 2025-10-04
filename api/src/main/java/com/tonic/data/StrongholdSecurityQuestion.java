package com.tonic.data;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.WidgetAPI;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.ArrayUtils;

@Getter
public enum StrongholdSecurityQuestion {
    QUESTION1("To pass you must answer me this: What do you do if someone asks you for your password or bank PIN to make you a member for free?",
            "Don't tell them anything and click the 'Report Abuse' button."),
    QUESTION2("To pass you must answer me this: You have been offered a free giveaway or double XP invitation via in-game chat or email. What should you do?",
            "Report the incident and do not click any links."),
    QUESTION3("To pass you must answer me this: You have been offered a free giveaway or double XP invitation via social media or a livestream. What should you do?",
            "Report the incident and do not click any links."),
    QUESTION4("To pass you must answer me this: Is it safe to get someone to level your account?",
            "No, you should never allow anyone to use your account."),
    QUESTION5("To pass you must answer me this: Hey adventurer! You've been randomly selected for a prize of 1 year of free membership! I'm just going to need some of your account details so I can put it on your account!",
            "No way! I'm reporting you to Jagex!"),
    QUESTION6("To pass you must answer me this: What is the best security step you can take to keep your registered email secure?",
            "Set up two-factor authentication with my email provider."),
    QUESTION7("To pass you must answer me this: What is the best way to secure your account?",
            "Two-factor authentication on your account and your registered email."),
    QUESTION8("To pass you must answer me this: How do I set a bank PIN?",
            "Talk to any banker."),
    QUESTION9("To pass you must answer me this: What should I do if I receive an email asking me to verify my identity or account details due to suspicious activity?",
            "a fake"),
    QUESTION10("To pass you must answer me this: Who can I give my password to?",
            "Nobody."),
    QUESTION11("To pass you must answer me this: What do I do if my account is compromised?",
            "Secure my device and reset my password."),
    QUESTION12("To pass you must answer me this: What do I do if a moderator asks me for my account details?",
            "Politely tell them no, then use the 'Report Abuse' button."),
    QUESTION13("To pass you must answer me this: A player trades you some valuable items, provides you a bond, then asks if you want to share your account so he can help you make progress. How do you respond?",
            "Decline the offer and report that player."),
    QUESTION14("To pass you must answer me this: Where is it safe to use my Old School RuneScape password?",
            "Only on the Old School RuneScape website."),
    QUESTION15("To pass you must answer me this: Whose responsibility is it to keep your account secure?",
            "Me."),
    QUESTION16("To pass you must answer me this: Psst! Adventurer! I've got a special offer for you, but you're going to have to trust me. If you give me some gold coins, I'll give you back twice whatever you gave me! How does that sound?",
            "No way! You'll just take my gold for your own! Reported!"),
    QUESTION17("To pass you must answer me this: Is it okay to buy an Old School RuneScape account?",
            "No, you should never buy an account."),
    QUESTION18("To pass you must answer me this: My friend asks me to for my password so that he can do a difficult quest for me. Do I give it to him?",
            "Don't give them my password."),
    QUESTION19("To pass you must answer me this: A player tells you to search for a video online, click the link in the description and comment on the forum post to win a cash prize. What do you do?",
            "Report the player for phishing."),
    QUESTION20("To pass you must answer me this: Adventurer, I'll trade items with you for an amazing price, but you've got to come immediately to a particular place on a different game world. Hurry up! Come now before you lose out! What do you say?",
            "Nope, you're tricking me into going somewhere dangerous."),
    QUESTION21("To pass you must answer me this: Which of these is an important characteristic of a secure password?",
            "It's never reused on other websites or accounts."),
    QUESTION22("To pass you must answer me this: You're watching a stream by someone claiming to be Jagex offering double XP. What do you do?",
            "Report the stream"),
    QUESTION23("To pass you must answer me this: Will Jagex prevent me from saying my PIN in game?",
            "No."),
    QUESTION24("To pass you must answer me this: A website claims that they can make me a player moderator. What should I do?",
            "Nothing, it's a fake."),
    QUESTION25("To pass you must answer me this: What do I do if I think I have a keylogger or virus?",
            "Virus scan my device then change my password."),
    QUESTION26("To pass you must answer me this: What should you do if another player messages you recommending a website to purchase items and/or gold?",
            "Do no visit the website and report the player who messaged you."),
    QUESTION27("To pass you must answer me this: Can I leave my account logged in while I'm out of the room?",
            "No."),
    QUESTION28("To pass you must answer me this: What do you do if someone asks you for your password or bank PIN to make you a player moderator?",
            "Don't give them the information and send an 'Abuse report'"),
    QUESTION29("To pass you must answer me this: What is an example of a good bank PIN?",
            "The birthday of a famous person or event."),
    QUESTION30("To pass you must answer me this: What should you do if your real-life friend asks for your password so he can check your stats?",
            "Don't give out your password to anyone. Not even close friends."),
    QUESTION31("To pass you must answer me this: A player starts asking you about very specific details linked to your account, such as when you created your account, your birthday date, internet provider etc. How should you react?",
            "Don't share your information and report the player."),
    QUESTION32("To pass you must answer me this: How do I remove a hijacker from my account?",
            "Use the Account Recovery system."),
    QUESTION33("To pass you must answer me this: You are part way through the Stronghold of Security when you have to answer another question. After you answer the question, you should...",
            "Read the text and follow the advice given."),
    QUESTION34("To pass you must answer me this: How do I set up two-factor authentication for my Old School RuneScape account?",
            "Through account settings on oldschool.runescape.com."),
    QUESTION35("To pass you must answer me this: Who is it okay to share my account with?",
    "Nobody")
    ;

    private static final int[] DOOR_HEADS = new int[] {
            NpcID.SOS_DOOR_WAR,
            NpcID.SOS_DOOR_DEATH,
            NpcID.SOS_DOOR_FAM,
            NpcID.SOS_DOOR_PEST
    };

    private static final int[] DOORS = new int[] {
            ObjectID.SOS_WAR_DOOR_FACE,
            ObjectID.SOS_WAR_DOOR_FACE_MIRR,
            ObjectID.SOS_DEATH_DOOR_FACE,
            ObjectID.SOS_DEATH_DOOR_FACE_MIRR,
            ObjectID.SOS_FAM_DOOR_FACE,
            ObjectID.SOS_FAM_DOOR_FACE_MIRR,
            ObjectID.SOS_PEST_DOOR_FACE,
            ObjectID.SOS_PEST_DOOR_FACE_MIRR
    };

    private final String question;
    private final String correctAnswer;

    StrongholdSecurityQuestion(String question, String correctAnswer) {
        this.question = question;
        this.correctAnswer = correctAnswer;
    }

    public static StrongholdSecurityQuestion fromQuestion(String question)
    {
        if(question == null || question.isEmpty())
            return null;
        question = question.toLowerCase().trim();
        for(StrongholdSecurityQuestion q : values())
        {
            if(q.getQuestion().toLowerCase().contains(question))
                return q;
        }
        return null;
    }

    public static boolean process(TileObjectEx door)
    {
        if(door == null || !ArrayUtils.contains(DOORS, door.getId()))
            return false;

        Logger.info("[Pathfinder] Interacting with '" + door.getName() + "'");
        Client client = Static.getClient();
        TileObjectAPI.interact(door, 0);
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        while (!DialogueAPI.dialoguePresent())
        {
            Delays.tick();
            if(!wp.equals(client.getLocalPlayer().getWorldLocation()))
            {
                return false;
            }
        }

        Widget head = WidgetAPI.get(InterfaceID.ChatLeft.HEAD);
        String question = null;
        String answer = null;

        if(head != null && ArrayUtils.contains(DOOR_HEADS, head.getModelId()))
        {
            question = replaceBrTags(WidgetAPI.getText(InterfaceID.ChatLeft.TEXT)).trim();
            StrongholdSecurityQuestion entry = fromQuestion(question);
            answer = entry != null ? entry.getCorrectAnswer() : null;
            if(answer != null && answer.contains("Why not upgrade to a Jagex Account for the latest two-factor"))
            {
                while (shouldWait())
                {
                    Delays.tick();
                }
                DialogueAPI.continueDialogue();
                Delays.tick();
                question = replaceBrTags(WidgetAPI.getText(InterfaceID.ChatLeft.TEXT)).trim();
                entry = fromQuestion(question);
                answer = entry != null ? entry.getCorrectAnswer() : null;
            }
        }

        Logger.norm("Stronghold Security Question Detected: " + question);
        while(DialogueAPI.dialoguePresent())
        {
            if(shouldWait())
            {
                Delays.tick();
                continue;
            }

            if(DialogueAPI.continueDialogue())
            {
                Delays.tick();
                continue;
            }

            if(answer == null)
            {
                DialogueAPI.selectOption(0);
                continue;
            }

            DialogueAPI.selectOption(answer);
            Delays.tick();
        }
        Delays.tick();
        return true;
    }

    private static boolean shouldWait()
    {
        Widget head = WidgetAPI.get(InterfaceID.ChatLeft.HEAD);
        if(head == null || !ArrayUtils.contains(DOOR_HEADS, head.getModelId()))
            return false;
        Widget cont = WidgetAPI.get(InterfaceID.ChatLeft.CONTINUE);
        if(cont == null || !WidgetAPI.isVisible(cont))
            return true;

        return !WidgetAPI.getText(cont).equals("Click here to continue");
    }

    private static String replaceBrTags(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder(input.length());
        int i = 0;
        int len = input.length();

        while (i < len) {
            if (i <= len - 4 &&
                    input.charAt(i) == '<' &&
                    input.charAt(i + 1) == 'b' &&
                    input.charAt(i + 2) == 'r' &&
                    input.charAt(i + 3) == '>') {

                boolean dashBefore = (i > 0 && input.charAt(i - 1) == '-');
                boolean dashAfter = (i + 4 < len && input.charAt(i + 4) == '-');

                if (!dashBefore && !dashAfter) {
                    result.append(' ');
                }

                i += 4;
            } else {
                result.append(input.charAt(i));
                i++;
            }
        }

        return result.toString();
    }
}