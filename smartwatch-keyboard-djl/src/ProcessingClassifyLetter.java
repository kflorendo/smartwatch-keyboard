import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;
import processing.core.PApplet;

import java.io.File;
import java.io.IOException;

public class ProcessingClassifyLetter extends PApplet {
    String absDir = System.getProperty("user.dir");
    final LetterClassifier letterClassifier = new LetterClassifier();

    // method for setting the size of the window
    public void settings(){
        size(224, 224);
    }

    // identical use to setup in Processing IDE except for size()
    public void setup(){
        background(0);
        fill(255,0,0);
//        rect(0,0,50,50);
//        System.out.println(ARGS_SKETCH_FOLDER);
//        System.out.println("Working Directory = " + System.getProperty("user.dir"));
//        stroke(255);
//        strokeWeight(10);
//        Tesseract tesseract = new Tesseract();
        try {
            letterClassifier.setup();
        } catch (ModelNotFoundException e) {
            throw new RuntimeException(e);
        } catch (MalformedModelException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void classify(){
        File myObj = new File("cur.jpeg");
        if (myObj.delete()) {
            System.out.println("deleted successful");
        } else {
            System.out.println("could not delete");
        }
        saveFrame(absDir + "/cur.jpeg");

        try {
            System.out.println(letterClassifier.classify("cur.jpeg"));
            System.out.println("done classifying");
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // clear background
//        background(0);
//        fill(0);
//        rect(0,0,224,224);
    }

    // identical use to draw in Prcessing IDE
    public void draw(){
        stroke(255);
        strokeWeight(8);
        if (mousePressed) {
//            System.out.print(sketchPath("data"));
            if (224 - 20 <= mouseX && mouseX <= 224 && 0 <= mouseY && mouseY <= 50) {
                background(0);
            }
            else if (0 <= mouseX && mouseX <= 50 && 0 <= mouseY && mouseY <= 50) {
                classify();
            } else {
                line(mouseX, mouseY, pmouseX, pmouseY);
            }

        }
//        line(0, 0, 500, 500);
//
    }


}