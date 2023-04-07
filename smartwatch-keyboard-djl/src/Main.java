import ai.djl.MalformedModelException;
import ai.djl.inference.*;
import ai.djl.modality.*;
import ai.djl.modality.cv.*;
import ai.djl.modality.cv.util.*;
import ai.djl.ndarray.*;
import ai.djl.repository.zoo.*;
import ai.djl.translate.*;
import ai.djl.training.util.*;
import ai.djl.util.*;

import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Main {


    public static void main(String[] args) throws IOException, ModelNotFoundException, MalformedModelException, TranslateException {
        String imagePath = "letter.jpeg";
        Image image = ImageFactory.getInstance().fromFile(Paths.get(imagePath));

        Path modelPath = Paths.get("converted_savedmodel/model.savedmodel");
        String modelName = "saved_model";
        image.getWrappedImage();

        Criteria<Image, Classifications> criteria =
                Criteria.builder()
                        .setTypes(Image.class, Classifications.class)
                        .optModelPath(modelPath)
                        .optModelName(modelName)
                        .optTranslator(new MyTranslator())
                        .optProgress(new ProgressBar())
                        .build();
        ZooModel model = criteria.loadModel();

        Predictor<Image, Classifications> predictor = model.newPredictor();

        System.out.println("starting predict");
        Classifications classifications = predictor.predict(image);

        System.out.println(classifications);
    }

}

class MyTranslator implements Translator<Image, Classifications> {

    private static final List<String> CLASSES = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z");

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        // Convert Image to NDArray
        Image image = input.resize(224, 224, false);
//        NDArray array = image.toNDArray(ctx.getNDManager(), Image.Flag.GRAYSCALE);
        NDArray array = image.toNDArray(ctx.getNDManager());
        System.out.println(array.getShape());
        return new NDList(NDImageUtils.toTensor(array));
    }

    @Override
    public Classifications processOutput(TranslatorContext ctx, NDList list) {
        // Create a Classifications with the output probabilities
        NDArray probabilities = list.singletonOrThrow().softmax(0);
        List<String> classNames = IntStream.range(0, 26).mapToObj(String::valueOf).collect(Collectors.toList());
        return new Classifications(CLASSES, probabilities);
    }

    @Override
    public Batchifier getBatchifier() {
        // The Batchifier describes how to combine a batch together
        // Stacking, the most common batchifier, takes N [X1, X2, ...] arrays to a single [N, X1, X2, ...] array
        return Batchifier.STACK;
    }
}