import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.*;
import ai.djl.translate.*;
import processing.core.PApplet;
import processing.core.PImage;

import java.io.IOException;
import java.util.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public final class Main extends PApplet {

    final LetterClassifier letterClassifier = new LetterClassifier();

    String[] phrases; //contains all of the phrases
    int totalTrialNum = 2; //the total number of phrases to be tested - set this low for testing. Might be ~10 for the real bakeoff!
    int currTrialNum = 0; // the current trial number (indexes into trials array above)
    float startTime = 0; // time starts when the first letter is entered
    float finishTime = 0; // records the time of when the final trial ends
    float lastTime = 0; //the timestamp of when the last trial was completed
    float lettersEnteredTotal = 0; //a running total of the number of letters the user has entered (need this for final WPM computation)
    float lettersExpectedTotal = 0; //a running total of the number of letters expected (correct phrases)
    float errorsTotal = 0; //a running total of the number of errors (when hitting next)
    String currentPhrase = ""; //the current target phrase
    String currentTyped = ""; //what the user has typed so far
    final int DPIofYourDeviceScreen = 128; //you will need to look up the DPI or PPI of your device to make sure you get the right scale!!
    //http://en.wikipedia.org/wiki/List_of_displays_by_pixel_density
    final float sizeOfInputArea = DPIofYourDeviceScreen*1; //aka, 1.0 inches square!
    PImage watch;

    // implementation variables
    int numAllLetters = 26;
    int numTopLetters = 5;
    int numAllLettersRounded = numAllLetters + numTopLetters - (numAllLetters % numTopLetters);
    List<String> letters = new ArrayList<String>(); // current letters displayed at top of screen
    List<String> allLetters = new ArrayList<String>(); // al letters returned by letter classifier
    int letterIndex = 0;

    // drawing variables
    final int screenWidth = 800;
    final int screenHeight = 800;
    final float drawAreaSize = sizeOfInputArea/5*4;
    final float screenTL_X = screenWidth/2-sizeOfInputArea/2;
    final float screenTL_Y = screenHeight/2-sizeOfInputArea/2;
    final float drawAreaX = screenWidth/2 - drawAreaSize/2;
    final float drawAreaY = screenTL_Y+sizeOfInputArea/5;
    final float arrowButtonWidth = sizeOfInputArea/8;
    final float letterButtonStartX = arrowButtonWidth;
    final float letterButtonWidth = (sizeOfInputArea - (2 * arrowButtonWidth)) / numTopLetters;
    final float letterButtonHeight = sizeOfInputArea / 5;
    final float leftButtonX = screenTL_X;
    final float rightButtonX = screenTL_X + arrowButtonWidth + numTopLetters * letterButtonWidth;
    final float sideButtonWidth = (sizeOfInputArea - drawAreaSize) / 2;
    final float sideButtonHeight = letterButtonHeight;
    final float deleteButtonX = screenTL_X;
    final float deleteButtonY = screenTL_Y + sizeOfInputArea - sideButtonHeight;
    final float spaceButtonX = screenTL_X + sizeOfInputArea - sideButtonWidth;
    final float spaceButtonY = screenTL_Y + sizeOfInputArea - sideButtonHeight;
    final float clearButtonX = screenTL_X + sizeOfInputArea - sideButtonWidth;
    final float clearButtonY = screenTL_Y + letterButtonHeight;

    public static void main(String[] args) {
        PApplet.main("Main");
    }

    // method for setting the size of the window
    public void settings(){
        size(screenWidth, screenHeight); //Sets the size of the app. You should modify this to your device's native size. Many phones today are 1080 wide by 1920 tall.
    }

    public void setup() {
        watch = loadImage("data/watchhand3smaller.png");
        phrases = loadStrings("data/phrases2.txt"); //load the phrase set into memory
        Collections.shuffle(Arrays.asList(phrases), new Random()); //randomize the order of the phrases with no seed
        //Collections.shuffle(Arrays.asList(phrases), new Random(100)); //randomize the order of the phrases with seed 100; same order every time, useful for testing

        orientation(LANDSCAPE); //can also be PORTRAIT - sets orientation on android device
        textFont(createFont("Arial", 24)); //set the font to arial 24. Creating fonts is expensive, so make difference sizes once in setup, not draw
        noStroke();

        try {
            letterClassifier.setup();
        } catch (ModelNotFoundException e) {
            throw new RuntimeException(e);
        } catch (MalformedModelException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        background(255); //clear background
        drawWatch(); //draw watch background
        drawDrawArea();

        for (int i = 0; i < numTopLetters; i++) {
            letters.add("");
        }

    }

    public void draw()
    {
        // background for text
        fill(255);
        rect(0,0,width,height/4);

        textSize(24);
        if (finishTime!=0)
        {
            fill(128);
            textAlign(CENTER);
            text("Finished", 280, 150);
            return;
        }

        if (startTime==0 & !mousePressed)
        {
            fill(128);
            textAlign(CENTER);
            text("Click to start time!", 280, 150); //display this messsage until the user clicks!
        }

        if (startTime==0 & mousePressed)
        {
            nextTrial(); //start the trials!
        }

        if (startTime!=0)
        {
            //feel free to change the size and position of the target/entered phrases and next button
            textAlign(LEFT); //align the text left
            fill(128);
            text("Phrase " + (currTrialNum+1) + " of " + totalTrialNum, 70, 50); //draw the trial count
            fill(128);
            text("Target:   " + currentPhrase, 70, 100); //draw the target string
            text("Entered:  " + currentTyped +"|", 70, 140); //draw what the user has entered thus far

            //draw very basic next button
            fill(255, 0, 0);
            rect(600, 600, 200, 200); //draw next button
            fill(255);
            text("NEXT > ", 650, 650); //draw next label

            // draw letter buttons
            textSize((int)(sizeOfInputArea / 9));
            for (int i = 0; i < numTopLetters; i++) {
                stroke(255);
                strokeWeight(1);
                fill(0);
                rect(letterButtonStartX + screenTL_X + i * letterButtonWidth, screenTL_Y, letterButtonWidth, letterButtonHeight);
                fill(255);
                text(letters.get(i).toUpperCase(), letterButtonStartX + screenTL_X + i * letterButtonWidth + letterButtonWidth / 3, screenTL_Y + letterButtonHeight / 4 * 3);
            }

            // draw left right arrow buttons
            fill(0);
            rect(leftButtonX, screenTL_Y, arrowButtonWidth, letterButtonHeight);
            fill(255);
            text("<", leftButtonX + arrowButtonWidth / 3, screenTL_Y + letterButtonHeight / 4 * 3);
            fill(0);
            rect(rightButtonX, screenTL_Y, arrowButtonWidth, letterButtonHeight);
            fill(255);
            text(">", rightButtonX + arrowButtonWidth / 3, screenTL_Y + letterButtonHeight / 4 * 3);

            // draw delete button
            fill(255,0,0);
            rect(deleteButtonX, deleteButtonY, sideButtonWidth, sideButtonHeight);

            // draw space button
            fill(0);
            rect(spaceButtonX, spaceButtonY, sideButtonWidth, sideButtonHeight);
            fill(255);
            text("_", spaceButtonX + sideButtonWidth / 4, spaceButtonY + sideButtonHeight / 4 * 3);

            // draw clear button
            fill(0);
            rect(clearButtonX, clearButtonY, sideButtonWidth, sideButtonHeight);
            fill(255);
            text("X", clearButtonX + sideButtonWidth / 4, clearButtonY + sideButtonHeight / 4 * 3);

            strokeWeight(0);

            // handle user drawing letters on draw area
            if (mousePressed && didMouseClick(drawAreaX, drawAreaY, drawAreaSize, drawAreaSize)) {
                stroke(255);
                strokeWeight(5);
                line(mouseX, mouseY, pmouseX, pmouseY);
                strokeWeight(0);
            }

        }
    }

    private void drawDrawArea() {
        fill(0);
        rect(drawAreaX, drawAreaY, drawAreaSize, drawAreaSize);
    }

    private boolean didMouseClick(float x, float y, float w, float h) //simple function to do hit testing
    {
        return (mouseX > x && mouseX<x+w && mouseY>y && mouseY<y+h); //check to see if it is in button bounds
    }

    public void classify(){
        String absDir = System.getProperty("user.dir");

        PImage screenshot = get((int) drawAreaX + 1, (int) drawAreaY + 1, (int) drawAreaSize, (int) drawAreaSize);
        screenshot.save(absDir + "/images/cur.jpeg");

        List<String> results;
        try {
            results = letterClassifier.classify("images/cur.jpeg");
            System.out.println(results);
            System.out.println("done classifying");
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        allLetters.clear();
        for (int i = 0; i < numAllLetters; i++) {
            allLetters.add(results.get(i));
        }
        for (int i = numAllLetters; i < numAllLettersRounded; i++) {
            allLetters.add("");
        }
        letterIndex = 0;
        letters = allLetters.subList(letterIndex, letterIndex + numTopLetters);
    }

    public void mouseReleased()
    {
        if (didMouseClick(drawAreaX, drawAreaY, drawAreaSize, drawAreaSize)) {
            classify();
        }
    }

    public void mousePressed()
    {
        // handle letter button click
        for (int i = 0; i < numTopLetters; i++) {
            if (didMouseClick(letterButtonStartX + screenTL_X + i * letterButtonWidth, screenTL_Y, letterButtonWidth, letterButtonHeight)) {
                currentTyped+=letters.get(i);
                drawDrawArea(); // clear
            }
        }
        // handle left arrow click
        if (didMouseClick(leftButtonX, screenTL_Y, arrowButtonWidth, letterButtonHeight)) {
            letterIndex = letterIndex == 0 ? numAllLettersRounded - numTopLetters : letterIndex - numTopLetters;
            letters = allLetters.subList(letterIndex, letterIndex + numTopLetters);
        }
        // handle right arrow click
        if (didMouseClick(rightButtonX, screenTL_Y, arrowButtonWidth, letterButtonHeight)) {
            letterIndex = (letterIndex + numTopLetters) % numAllLettersRounded;
            letters = allLetters.subList(letterIndex, letterIndex + numTopLetters);
        }
        // handle delete click
        if (didMouseClick(deleteButtonX, deleteButtonY, sideButtonWidth, sideButtonHeight)) {
            currentTyped = currentTyped.substring(0, currentTyped.length()-1);
        }
        // handle space click
        if (didMouseClick(spaceButtonX, spaceButtonY, sideButtonWidth, sideButtonHeight)) {
            currentTyped += " ";
        }
        // handle clear click
        if (didMouseClick(clearButtonX, clearButtonY, sideButtonWidth, sideButtonHeight)) {
            drawDrawArea();
        }

        //You are allowed to have a next button outside the 1" area
        if (didMouseClick(600, 600, 200, 200)) //check if click is in next button
        {
            nextTrial(); //if so, advance to next trial
        }
    }


    private void nextTrial()
    {
        if (currTrialNum >= totalTrialNum) //check to see if experiment is done
            return; //if so, just return

        if (startTime!=0 && finishTime==0) //in the middle of trials
        {
            System.out.println("==================");
            System.out.println("Phrase " + (currTrialNum+1) + " of " + totalTrialNum); //output
            System.out.println("Target phrase: " + currentPhrase); //output
            System.out.println("Phrase length: " + currentPhrase.length()); //output
            System.out.println("User typed: " + currentTyped); //output
            System.out.println("User typed length: " + currentTyped.length()); //output
            System.out.println("Number of errors: " + computeLevenshteinDistance(currentTyped.trim(), currentPhrase.trim())); //trim whitespace and compute errors
            System.out.println("Time taken on this trial: " + (millis()-lastTime)); //output
            System.out.println("Time taken since beginning: " + (millis()-startTime)); //output
            System.out.println("==================");
            lettersExpectedTotal+=currentPhrase.trim().length();
            lettersEnteredTotal+=currentTyped.trim().length();
            errorsTotal+=computeLevenshteinDistance(currentTyped.trim(), currentPhrase.trim());
        }

        //probably shouldn't need to modify any of this output / penalty code.
        if (currTrialNum == totalTrialNum-1) //check to see if experiment just finished
        {
            finishTime = millis();
            System.out.println("==================");
            System.out.println("Trials complete!"); //output
            System.out.println("Total time taken: " + (finishTime - startTime)); //output
            System.out.println("Total letters entered: " + lettersEnteredTotal); //output
            System.out.println("Total letters expected: " + lettersExpectedTotal); //output
            System.out.println("Total errors entered: " + errorsTotal); //output

            float wpm = (lettersEnteredTotal/5.0f)/((finishTime - startTime)/60000f); //FYI - 60K is number of milliseconds in minute
            float freebieErrors = (float) (lettersExpectedTotal*.05); //no penalty if errors are under 5% of chars
            float penalty = max(errorsTotal-freebieErrors, 0) * .5f;

            System.out.println("Raw WPM: " + wpm); //output
            System.out.println("Freebie errors: " + freebieErrors); //output
            System.out.println("Penalty: " + penalty);
            System.out.println("WPM w/ penalty: " + (wpm-penalty)); //yes, minus, becuase higher WPM is better
            System.out.println("==================");

            currTrialNum++; //increment by one so this mesage only appears once when all trials are done
            return;
        }

        if (startTime==0) //first trial starting now
        {
            System.out.println("Trials beginning! Starting timer..."); //output we're done
            startTime = millis(); //start the timer!
        }
        else
            currTrialNum++; //increment trial number

        lastTime = millis(); //record the time of when this trial ended
        currentTyped = ""; //clear what is currently typed preparing for next trial
        currentPhrase = phrases[currTrialNum]; // load the next phrase!
        //currentPhrase = "abc"; // uncomment this to override the test phrase (useful for debugging)
    }


    private void drawWatch()
    {
        float watchscale = (float) (DPIofYourDeviceScreen/138.0);
        pushMatrix();
        translate(width/2, height/2);
        scale(watchscale);
        imageMode(CENTER);
        image(watch, 0, 0);
        popMatrix();
    }

    //=========SHOULD NOT NEED TO TOUCH THIS METHOD AT ALL!==============
    private int computeLevenshteinDistance(String phrase1, String phrase2) //this computers error between two strings
    {
        int[][] distance = new int[phrase1.length() + 1][phrase2.length() + 1];

        for (int i = 0; i <= phrase1.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= phrase2.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= phrase1.length(); i++)
            for (int j = 1; j <= phrase2.length(); j++)
                distance[i][j] = min(min(distance[i - 1][j] + 1, distance[i][j - 1] + 1), distance[i - 1][j - 1] + ((phrase1.charAt(i - 1) == phrase2.charAt(j - 1)) ? 0 : 1));

        return distance[phrase1.length()][phrase2.length()];
    }

}