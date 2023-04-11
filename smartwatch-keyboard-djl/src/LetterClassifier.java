import ai.djl.MalformedModelException;
import ai.djl.inference.*;
import ai.djl.modality.*;
import ai.djl.modality.cv.*;
import ai.djl.modality.cv.util.*;
import ai.djl.ndarray.*;
import ai.djl.repository.zoo.*;
import ai.djl.translate.*;
import ai.djl.training.util.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import java.util.Arrays;

public class LetterClassifier {
    Predictor<Image, Classifications> predictor;
    int numTop = 26;
    public void setup() throws ModelNotFoundException, MalformedModelException, IOException {
        Path modelPath = Paths.get("models/modelv2/model.savedmodel");
        String modelName = "saved_model";

        Criteria<Image, Classifications> criteria =
                Criteria.builder()
                        .setTypes(Image.class, Classifications.class)
                        .optModelPath(modelPath)
                        .optModelName(modelName)
                        .optTranslator(new LetterTranslator())
                        .optProgress(new ProgressBar())
                        .build();
        ZooModel model = criteria.loadModel();

        predictor = model.newPredictor();
        System.out.println("letter classifier setup successful");
    }
    public List<String> classify(String imagePath) throws IOException, TranslateException {
        Image image = ImageFactory.getInstance().fromFile(Paths.get(imagePath));

        System.out.println("starting predict");
        Classifications classifications = predictor.predict(image);

        List<String> topLetters = new ArrayList<String>();
        List<Classifications.Classification> topClassifications = classifications.topK(numTop);
        for (int i = 0; i < numTop; i++) {
            topLetters.add(topClassifications.get(i).getClassName());
        }
        return topLetters;
    }
}

class LetterTranslator implements Translator<Image, Classifications> {

    private static final List<String> CLASSES = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z");

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) throws IOException {
        // Convert Image to NDArray
        NDArray array = input.toNDArray(ctx.getNDManager());
        array = NDImageUtils.resize(array, 28, 28, Image.Interpolation.AREA);
        array = NDImageUtils.resize(array, 224, 224, Image.Interpolation.AREA);

//        array = array.div(0.5);
//        array = array.sub(1);

//        Image after = ImageFactory.getInstance().fromNDArray(array);
//        OutputStream out = new FileOutputStream("processed.jpeg");
//        after.save(out, "jpeg");

        return new NDList(NDImageUtils.toTensor(array));
    }

    @Override
    public Classifications processOutput(TranslatorContext ctx, NDList list) {
        // Create a Classifications with the output probabilities
//        NDArray probabilities = list.singletonOrThrow().softmax(0);
        NDArray probabilities = list.singletonOrThrow();
        return new Classifications(CLASSES, probabilities);
    }

    @Override
    public Batchifier getBatchifier() {
        // The Batchifier describes how to combine a batch together
        // Stacking, the most common batchifier, takes N [X1, X2, ...] arrays to a single [N, X1, X2, ...] array
        return Batchifier.STACK;
    }
}
