package main;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;

import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.filter2D;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Controller implements Initializable {
    // связи элементов окна с переменными по имени fx:id
    @FXML
    private ImageView originalImg;
    @FXML
    private ImageView grayscaleImg;
    @FXML
    private ImageView apply1Img;
    @FXML
    private ImageView apply2Img;
    @FXML
    private Label preset1;
    @FXML
    private Label preset2;
    @FXML
    private Label spRangeLbl;

    @FXML // инциализация спиннеров (filter matrix)
    List<Spinner> spArray;                  // список элементов окна типа Spinner
    @FXML                                   // инциализация спиннеров (filter matrix)
    Spinner spOffset;                       // элемент spOffset - смещение delta при фильтрации
    @FXML                                   // инциализация спиннеров (filter matrix)
    Spinner spTreshold1, spTreshold2;       // Порог поиска контуров Кенни
    int filterSize;                         // размер матрицы свёртки
    String presetTxt;                       // наименование пресета для вьюпорта Apply 1 и 2
    int minVal = -256, maxVal = 256;        // граничные значения спиннеров
    int treshold1 = 10, treshold2 = 100;    // порог обнаружения контруров
    Mat imgSrcMat, imgGrayscaleMat;         // матрицы оригинала и в оттенках серого изображений
    Mat filterMatrixMat;                    // матрица свёртки
    Mat apply1Mat, apply2Mat;               // для записи на диск
    // private final Desktop desktop = Desktop.getDesktop(); // для открытия файла сопоставленным по типу приложением

    private final String tempDir = "C:/__tmp"; // временный каталог для решения проблем с кириллицей в путях файлов в OpenCV
    // поиск в папке ресурсов, если подключено в fxml
    // InputStream picEmpty = getClass().getResourceAsStream("../resources/empty_img.png");
    // Image saveImg = new Image(picEmpty);
    // Mat emptySaveImgMat = Imgcodecs.imread("../resources/empty_img.png", Imgcodecs.IMREAD_UNCHANGED); // для установки в слоты
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss > "); // формат даты

    @FXML
    public void zeroMatrix() {
        /**
         *  Пресеты: обнуление матрицы спинеров.
         *  Обработчик кнопки.
         *  Установка значений в спиннер-контролы.
         */
        System.out.println("\nПресет: обнуление матрицы спинеров\n");
        presetSpinnerMartrix(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        presetTxt = "zero matrix";
    }
    @FXML
    public void negative() {
        /**
         *  Пресеты: негатив.
         *  Обработчик кнопки.
         *  Установка значений в спиннер-контролы.
         */
        System.out.println("\nПресет: негатив\n");
        presetSpinnerMartrix(new int[] {0, 0, 0, 0, -1, 0, 0, 0, 0, 256});
        presetTxt = "negative";
    }
    @FXML
    public void blur() {
        /**
         *  Пресеты: размытие.
         *  Обработчик кнопки.
         *  Установка значений в спиннер-контролы.
         */
        System.out.println("\nПресет: размытие\n");
        presetSpinnerMartrix(new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 0});
        presetTxt = "blur";
    }
    @FXML
    public void lightBlur() {
        /**
         *  Пресеты: легкое размытие.
         *  Обработчик кнопки.
         *  Установка значений в спиннер-контролы.
         */
        System.out.println("\nПресет: легкое размытие\n");
        presetSpinnerMartrix(new int[] {1, 1, 0, 1, 1, 0, 0, 0, 0, 0});
        presetTxt = "light blur";
    }
    @FXML
    public void sharpen() {
        /**
         *  Пресеты: резкость.
         *  Обработчик кнопки.
         *  Установка значений в спиннер-контролы.
         */
        System.out.println("\nПресет: резкость\n");
        presetSpinnerMartrix(new int[] {0, -1, 0, -1, 5, -1, 0, -1, 0, 0});
        presetTxt = "sharpen";
    }
    @FXML
    public void lightSharpen() {
        /**
         *  Пресеты: легкая резкость.
         *  Обработчик кнопки.
         *  Установка значений в спиннер-контролы.
         */
        System.out.println("\nПресет: легкая резкость\n");
        presetSpinnerMartrix(new int[] {-1, 0, 0, 0, 2, 0, 0, 0, 0, 0});
        presetTxt = "light sharpen";
    }
    @FXML
    public void emboss() {
        /**
         *  Пресеты: тиснение.
         *  Обработчик кнопки.
         *  Установка значений в спиннер-контролы.
         */
        System.out.println("\nПресет: тиснение\n");
        presetSpinnerMartrix(new int[] {-2, -1, 0, -1, 1, 1, 0, 1, 2, 0});
        presetTxt = "emboss";
    }
    @FXML
    public void lightEmboss() {
        /**
         *  Пресеты: легкое тиснение.
         *  Обработчик кнопки.
         *  Установка значений в спиннер-контролы.
         */
        System.out.println("\nПресет: легкое тиснение\n");
        presetSpinnerMartrix(new int[] {1, 0, 0, 0, 1, 0, 0, 0, -1, 0});
        presetTxt = "light emboss";
    }
    @FXML
    public void canny() {
        /**
         *  Выделение контуров методом Кенни.
         *  Обработчик кнопки.
         *  Установка итогового изображения в Slot2.
         */
        // (OpenCV Java, Прохорёнок Н.А., с.223)
        System.out.println("\nМетод Кенни: выделение контуров. Порог: " +
                spTreshold1.getValue().toString() + " ... " + spTreshold2.getValue().toString() + "\n");
        preset1.setText("Canny edges");
        // для Canny edges не должен быть пустым
        apply1Mat = imgGrayscaleMat.clone();
        Canny(imgGrayscaleMat, apply1Mat, (int) spTreshold1.getValue(), (int) spTreshold2.getValue());
        // установка изображения в слот интерфейса окна, как результат операции
        apply1Img.setImage(SwingFXUtils.toFXImage(MatToBufferedImage(apply1Mat), null));
    }
    @FXML
    public void about() throws Exception {
        /**
         *  Вывод окна с информацией о программе.
         *  Обработчик кнопки.
         */
        System.out.println("\nО программе\n");
        // вывод окна about
        Stage stage = new Stage();
        Main.window_init(stage,"About program...","../resources/about");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("../resources/icon.jpg")));// вывод иконки окна
    }
    @FXML
    public void load_image() throws IOException { // mouseEvent../resources/empty_img.png
        /**
         *  Загрузка изображения.
         *  Обработчик кнопки.
         */
        // загрузка изображений (Прохоренок Н.А.)
        // public static Mat imread(String filename);
        // public static Mat imread(String filename, int flags);  // сигнатура вызова
        List<File> files = choiceFileDialog("load");        // список файлов
        Path tmpPath = setTempPath(files.get(0), "load");   // копирование файла в path на латиннице (OpenCV не понимает кириллицу в пути)
        Image image = new Image(tmpPath.toUri().toString());      // формат пути: file:/C:/folder/file.jpg
        originalImg.setImage(image);                              // установка изображения в слот

        // преобразования изображения к матрице
        imgSrcMat = Imgcodecs.imread(tmpPath.toString(), Imgcodecs.IMREAD_UNCHANGED); // оригинальное изображение
        imgGrayscaleMat = Imgcodecs.imread(tmpPath.toString(), Imgcodecs.IMREAD_GRAYSCALE); // оттенки серого
        if (imgSrcMat.empty()) {
            // кириллица в пути в OpenCV недопустима и решена размещением файлов во временном каталоге с удалением после чтения файла
            System.out.println("Изображение не загружено!");
        } else {
            img_info(imgSrcMat);
            // установка изображения в слот интерфейса окна
            BufferedImage grayscaleBufImg = MatToBufferedImage(imgGrayscaleMat);
            grayscaleImg.setImage(SwingFXUtils.toFXImage(grayscaleBufImg, null));
        }
        delTempPath(tmpPath); // удаление временного каталога с файлом изображения
    }
    @FXML
    public void save_grayscale() throws IOException {
        /**
         *   Сохранение изображения (в градациях серого) на диск.
         *   Обработчик кнопки.
         */
        saveFile(imgGrayscaleMat);
    }
    @FXML
    public void save_apply1() throws IOException {
        /**
         *  Сохранение изображения в Slot1 на диск.
         *  Обработчик кнопки.
         */
        saveFile(apply1Mat);
    }
    @FXML
    public void save_apply2() throws IOException {
        /**
         *  Cохранение изображения в Slot2 на диск.
         *  Обработчик кнопки.
         */
        saveFile(apply2Mat);
    }
    @FXML
    public void apply1() {
        /**
         *  Применение фильтра к изображению и установка результата в Slot1.
         *  Обработчик кнопки.
         */
        apply1Mat = apply(apply1Img);
        preset1.setText(presetTxt);
    }
    @FXML
    public void apply2() {
        /**
         *  Применение фильтра к изображению и установка результата в Slot2.
         *  Обработчик кнопки.
         */
        apply2Mat = apply(apply2Img);
        preset2.setText(presetTxt);
    }

    /* способ активизации шаблона .fxml
     @FXML
     private void panel() throws IOException {
        Main.setRoot("spatial_filter");
     }
     @param mouseEvent
     */

    @Override //@FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {
        /**
         * Initializes the controller class. This method is automatically called
         * after the fxml file has been loaded.
         */
        filterSize = (int) Math.sqrt(spArray.size());
        initSpinners();
        spRangeLbl.setText(spRangeLbl.getText() + " [" + minVal + ", " + maxVal + "]");
    }

    private void presetSpinnerMartrix(int[] arr) {
        /**
         * Установка пресетов значений матрицы фильтра и смещения Offset
         */
        int i;
        for (i=0; i < spArray.size(); i++) {
            spArray.get(i).getValueFactory().setValue(arr[i]);  // матрица
        }
        spOffset.getValueFactory().setValue(arr[i]);            // смещение
    }

    private Mat apply(ImageView applyImg) {
        /**
         *  Базовый метод обработки изображений с помощью матрицы фильтра.
         *  Возвращает изображение в формате Mat.
         */
        Mat fmatr = initFilterMatrix(spArray);
        viewMatrix(fmatr);
        Mat applyMat = filter(imgSrcMat, new Mat(), fmatr); //imgSrcMat.rows(), imgSrcMat.cols(), imgSrcMat.type() фильтрация изображения матрицей свёртки
        // установка изображения в слот интерфейса окна
        applyImg.setImage(SwingFXUtils.toFXImage(MatToBufferedImage(applyMat), null));
        return applyMat;
    }

    public Mat filter(Mat imgGrayscaleMat, Mat imgDst, Mat kernel) { // imgSrc
        /**
         *  Преобразование изображения с помощью фильтра.
         *  Возвращает изображение в формате Mat.
         */
        // метод filter2D() - фильтр с произвольными значениями на основе мартицы свёртки
        // (OpenCV Java, Прохорёнок Н.А., с.200)
        int ddepth = -1; // глубина целевого изображения - глубина оригинала
        Point anchor = new Point(-1, -1); // координаты ядра свёртки - центр матрицы
        double delta = 1.0 * (int)spOffset.getValueFactory().getValue(); // offset - прибавление к результату
        // тип рамки вокруг изображения (разд. 4.9). По умолчанию - BORDER_DEFAULT // borderInterpolate - интерполяция
        int borderType = Core.BORDER_REPLICATE; // BORDER_REPLICATE — повтор крайних пикселов
        // фильтрация
        filter2D(imgGrayscaleMat, imgDst, ddepth, kernel, anchor, delta, borderType); //imgSrc 256
        return imgDst;
    }

    private Mat initFilterMatrix(List<Spinner> matrix) {
        /**
         *  Инициализация матрицы фильтра значениями (после нажатия на Apply).
         */
        // Матрица свёртки из массива спиннеров
        filterMatrixMat = new Mat(filterSize, filterSize, CvType.CV_32FC1); // imgGrayscaleMat.type() -> CV_8UC1 -> unsigned одноканальное
        double[] data = new double[matrix.size()];                          // данные для матрицы
        System.out.print("\nВходные элементы фильтра (data):\n");
        for (int i=0; i<matrix.size(); i++) {
            data[i] = 1.0 * (int) spArray.get(i).getValue();
            System.out.print("  " + data[i] + (((i+1) % filterSize == 0) ? "\n" : ""));
        }
        filterMatrixMat.put(0, 0, normalized(data)); //
        return filterMatrixMat;
    }

    double arrSumm(double[] arr) {
        /**
         *  Возвращает сумму элементов массива.
         */
        double sum = 0;
        for (double v : arr) {
            sum += v;
        }
        return Math.abs(sum);
    }

    private double[] normalized(double[] data) {
        /**
         *  Нормализация матрицы свёртки.
         *  Возвращает норализованный массив.
         */
        // поиск суммы элементов
        double sum = arrSumm(data);
        System.out.print("Модуль суммы элементов: " + sum + "\n");
        // нормализация
        System.out.print("\nНормированные элементы фильтра (data):\n");
        for (int i=0; i<data.length; i++) {
            data[i] = (sum != 0) ? data[i]/sum : 0;
            System.out.print("  " + data[i] + (((i+1) % filterSize == 0) ? "\n" : ""));
        }
        sum = arrSumm(data);
        System.out.print("Модуль суммы элементов [0...1]: " + sum + "\n");
        return data;
    }

    void initSpinners() {
        /**
         *  Инициализация Spinner-контролов.
         */
        Pattern p = Pattern.compile("^-?\\d+$"); // целые числа (в т.ч. отрицательные) // (\d+\.?\d*)? - вещественные чисел
        // добавление в массив спиннер-контролов Offset, Treshold для инициализации фабрики значений спиннеров
        spArray.add(spOffset);
        spArray.add(spTreshold1);
        spArray.add(spTreshold2);
        // Инициализация спиннеров
        for (Spinner spinner : spArray) {
            // параметры спиннера // new Spinner(-5, 5, 1, 2)); // min, max, initial, step
            spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(minVal, maxVal, 0, 1));
            spinner.setEditable(true); // ввод пользовательских значений
            spinner.getValueFactory().setConverter(
                    new StringConverter() { // проверка на введённое значение и -> конвертация значения
                        @Override
                        public String toString(Object obj) {
                            return (obj == null) ? "0" : obj.toString();
                        }

                        @Override
                        public Integer fromString(String s) {
                            if (s.matches(p.toString())) {
                                try {
                                    return Integer.valueOf(s);
                                } catch (NumberFormatException e) {
                                    return 0;
                                }
                            }
                            return 0;
                        }
                    }
            );
            //System.out.println("sp" + i + ": " + spArray.get(i).getValue());
            // Дополнительные обработчики ввода (работают аналогично конвертеру)
            /*
            int finalI = i; // счётчик -> в константу для использования внутри обработчиков
            spArray.get(i).valueProperty().addListener((obs, oldValue, newValue) -> {
                System.out.println("sp" + finalI + " = " + spArray.get(finalI).getValue());
                if (!p.matcher(spArray.get(finalI).getValue().toString()).matches())
                    spArray.get(finalI).getValueFactory().setValue(oldValue);
            });
            */
            // 1. Проверка по regexp (когда он вызывается?)
            /*
            spArray.get(i).valueProperty().addListener((observable, oldValue, newValue) -> {
                if (!p.matcher(newValue.toString()).matches())
                    spArray.get(finalI).getValueFactory().setValue(oldValue);
                System.out.println("sp" + finalI + " = " + spArray.get(finalI).getValue()); // вывод значения при изменении
            });
            */
            /*
            // 2. Потеря фокуса
            spArray.get(i).focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    spArray.get(finalI).increment(0); // won't change value, but will commit editor
                }
                System.out.println("sp" + finalI + " = " + spArray.get(finalI).getValue()); // вывод значения при изменении
            });
            */
            // событие спиннеров - изменение значения
            spinner.valueProperty().addListener((observable, oldValue, newValue) -> presetTxt = "user defined");
        }
        // исключение Offset и Treshold из массива
        spArray.remove(spOffset);
        spArray.remove(spTreshold1);
        spArray.remove(spTreshold2);
        spTreshold1.getValueFactory().setValue(treshold1);
        spTreshold2.getValueFactory().setValue(treshold2);
    }

    private void viewMatrix(Mat matrix){
        /**
         *  Вывод в консоль значений матрицы фильтра.
         */
        System.out.println("\nМатрица свёртки (Mat) " + matrix.size() + " типа " + CvType.typeToString(matrix.type()) + ":\n"
                + matrix.dump());
        System.out.println("Offset = " + spOffset.getValue());
        /**
        for (int j = 0, r = matrix.rows(); j < r; j++) {
            for (int i = 0, c = matrix.cols(); i < c; i++) {
                System.out.printf(matrix.get(j, i) + " ");
            }
            System.out.println();
        }
        */
    }

    private void img_info(Mat imgMat) {
        /**
         *  Вывод в консоль характеристик изображения.
         */
        System.out.println("Тип: " + CvType.typeToString(imgMat.type())
                + " | Размер: " + imgMat.width() + " x " + imgMat.height()
                + " | Каналы: " + imgMat.channels());
    }

    public List<File> choiceFileDialog(String mode) {   // ActionEvent event
        /**
         *  Вызов диалогового окна загрузки и сохранения файлов.
         */
        File file = new File("");              // загруженный файл
        List<File> flist = new ArrayList<>();           // список файлов
        FileChooser fileChooser = new FileChooser();    // Класс работы с диалогом выборки и сохранения
        /*
         // для одного варианта выбора типа
         fileChooser.setTitle("Open Image");            // Заголовок диалога
         FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("HTML files (*.html)", "*.html"); // Расширение
         fileChooser.getExtensionFilters().add(extFilter);
         */

        configureFileChooser(fileChooser); // конфигурация параметров окна выбора файла
        switch (mode) {
            case "load" -> { // Открытие файла
                fileChooser.setTitle("Open file"); // заголовок диалога
                flist.add(fileChooser.showOpenDialog(Main.primaryStage)); // Указываем окно текущей сцены
                if (flist.size() != 0) {
                    /*
                    if (openFile(file)) {
                    }
                    // else -> вывод ошибки из openFile()
                    */
                    System.out.println(dateFormat.format(new Date()) + "Выбран файл: " + flist.get(0).getAbsolutePath());
                }
            }
            case "multiple" -> { // загрузка нескольких файлов - showOpenMultipleDialog
                fileChooser.setTitle("Open some files"); // заголовок диалога
                flist.addAll(fileChooser.showOpenMultipleDialog(Main.primaryStage));
                if (flist.size() != 0) {
                    /*
                    for (File nextfile : flist) {
                        openFile(nextfile);
                    }*/
                    System.out.println(dateFormat.format(new Date()) + "Выбрано несколько файлов из: " + flist.get(0).getParent());
                }
            }
            case "save" -> { // сохранение файла
                fileChooser.setTitle("Save the file"); // заголовок диалога
                flist.add(file = fileChooser.showSaveDialog(Main.primaryStage));
            }
        }
        return flist;
    }

    // Не применяется
    /*
    private boolean openFile(File file) { // Открыть файл (открывает системное проиложение, сопоставленное с типом файла)
        boolean result = true;
        try {
            desktop.open(file);
        } catch (IOException ex) {
            Logger.getLogger(
                    Main.class.getName()).log(
                    Level.SEVERE, null, ex
            );
            System.out.println(ex.getMessage()); // сообщение об ошибке
            result = false;
        }
        return result;
    }*/

    private boolean saveFile(Mat matImg) throws IOException {
        /**
         *  Сохранение файлов.
         *  Возвращает результат сохранения в формате boolean.
         */
        boolean saved;
        List<File> files = choiceFileDialog("save"); // список файлов
        Path tmpPath = setTempPath(files.get(0), "save"); // копирование файла в path на латиннице (OpenCV не понимает кириллицу в пути)
        Path to = FileSystems.getDefault().getPath(files.get(0).getAbsolutePath());
        /*
        if (files.size() != 0) {
            try { // сохранение изображения saveImg
                ImageIO.write(SwingFXUtils.fromFXImage(saveImg, null), getFileExtension(files.get(0)), tmpPath.toFile());
                saved = true;
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
        */
        saved = Imgcodecs.imwrite(tmpPath.toString(), matImg); // сохранение во временный каталог без кириллицы в Path
        Files.copy(tmpPath, to, REPLACE_EXISTING); // to.resolve(source.getFileName())
        System.out.println(dateFormat.format(new Date())
                + (saved ? "Изображение сохранено в " + files.get(0).toString(): "Не удалось сохранить изображение!"));

        delTempPath(tmpPath); // удаление временного каталога с файлом изображения
        return saved;
    }

    Path setTempPath(File file, String mode) throws IOException {
        /**
         *  Создание временного каталога с файлом.
         */
        Path source, to;
        File tmpPath = new File(tempDir);
        if (tmpPath.exists() || tmpPath.mkdir()) {
            switch (mode) { // enhanсed swith
                case "load" -> {
                    source = FileSystems.getDefault().getPath(file.getAbsolutePath());
                    to = FileSystems.getDefault().getPath(tmpPath.getPath(), "__image." + getFileExtension(file));
                    System.out.println(dateFormat.format(new Date()) + "Создан временный каталог с файлом: " + file.toString());
                    return Files.copy(source, to, REPLACE_EXISTING); // to.resolve(source.getFileName())
                }
                case "save" -> {
                    source = FileSystems.getDefault().getPath(tmpPath.getPath(), "__image." + getFileExtension(file));
                    System.out.println(dateFormat.format(new Date()) + "Создан временный каталог: " + tmpPath.toString());
                    return source;
                }
            }
        } else {
            System.out.println("Временный каталог не создан!");
        }
        return null;
    }

    void delTempPath(Path tmpPath) {
        /**
         *  Удаление временного каталога с файлом.
         */
        File file = new File(tmpPath.toString());
        if (file.exists()) {
            if (file.delete() && new File(file.getParent()).delete()) { // удаляется файл, затем каталог
                System.out.println(dateFormat.format(new Date()) + "Временный каталог удалён: " + file.getParent());
            } else {
                System.out.println("Невозможно удалить временный файл и/или каталог: " + file.getAbsolutePath());
            }
        } else {
            System.out.println("Временный каталог не существует.");
        }
    }

    private static String getFileExtension(File file) {
        /**
         *  Возвращает расширение файла.
         */
        String fileName = file.getName();
        // если в имени файла есть точка и она не является первым символом в названии файла
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            // то возвращаем все знаки после последней точки в названии файла: ХХХХХ.txt -> txt
            return fileName.substring(fileName.lastIndexOf(".") + 1);
            // иначе - расширения нет
        else return "";
    }

    private static void configureFileChooser(FileChooser fileChooser) {
        /**
         *  Конфигурация окна открытия файла.
         */
        // Set Initial Directory
        fileChooser.setInitialDirectory(new File("C:/"));
        // Add Extension Filters
        fileChooser.getExtensionFilters().addAll( // типы загружаемых файлов
                new FileChooser.ExtensionFilter("All Files", "*.*"),
                // JPEG files
                new FileChooser.ExtensionFilter("JP2", "*.jp2"), // JPEG 2000 files
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("JPE", "*.jpe"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpeg"),
                // Windows bitmaps
                new FileChooser.ExtensionFilter("BMP", "*.bmp"),
                new FileChooser.ExtensionFilter("DIB", "*.dib"),
                // TIFF files
                new FileChooser.ExtensionFilter("TIF", "*.tif"),
                new FileChooser.ExtensionFilter("TIFF", "*.tiff"),
                // Portable Network Graphics
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                // Portable image format
                new FileChooser.ExtensionFilter("PPM", "*.ppm"),
                new FileChooser.ExtensionFilter("PGM", "*.pgm"),
                new FileChooser.ExtensionFilter("PBM", "*.pbm"),
                // Sun roasters
                new FileChooser.ExtensionFilter("SM", "*.sm"),
                // OpenCV version 3.3.0
                new FileChooser.ExtensionFilter("PIC", "*.pic"),
                new FileChooser.ExtensionFilter("PXM", "*.PXM"),
                new FileChooser.ExtensionFilter("PNM", "*.pnm"),
                new FileChooser.ExtensionFilter("HDR", "*.hdr"),
                new FileChooser.ExtensionFilter("EXR", "*.exr"),
                new FileChooser.ExtensionFilter("WEBP", "*.webp") //,
        );
    }

    // методы преобразования изображения (Прохорёнок Н.А., с.104-105)
    public static BufferedImage MatToBufferedImage(Mat m) {
        /**
         *  Преобразование изображения в формате матрицы (Mat) в буфериророванное (BufferedImage).
         */
        if (m == null || m.empty()) return null;
        if (m.depth() == CvType.CV_8U) {}
        else if (m.depth() == CvType.CV_16U) { // CV_16U => CV_8U
            Mat m_16 = new Mat();
            m.convertTo(m_16, CvType.CV_8U, 255.0 / 65535);
            m = m_16;
        }
        else if (m.depth() == CvType.CV_32F) { // CV_32F => CV_8U
            Mat m_32 = new Mat();
            m.convertTo(m_32, CvType.CV_8U, 255);
            m = m_32;
        }
        else
            return null;
        int type;
        if (m.channels() == 1)
            type = BufferedImage.TYPE_BYTE_GRAY;
        else if (m.channels() == 3)
            type = BufferedImage.TYPE_3BYTE_BGR;
        else if (m.channels() == 4)
            type = BufferedImage.TYPE_4BYTE_ABGR;
        else
            return null;
        byte[] buf = new byte[m.channels() * m.cols() * m.rows()];
        m.get(0, 0, buf);
        byte tmp;
        if (m.channels() == 4) { // BGRA => ABGR
            for (int i = 0; i < buf.length; i += 4) {
                tmp = buf[i + 3];
                buf[i + 3] = buf[i + 2];
                buf[i + 2] = buf[i + 1];
                buf[i + 1] = buf[i];
                buf[i] = tmp;
            }
        }
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        byte[] data =
                ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buf, 0, data, 0, buf.length);
        return image;
    }

    public static Mat BufferedImageToMat(BufferedImage img) {
        /**
         *  Преобразование изображения в формате буфериророванного (BufferedImage) в матрицу (Mat).
         */
        if (img == null) return new Mat();
        int type;
        if (img.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            type = CvType.CV_8UC1;
        }
        else if (img.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            type = CvType.CV_8UC3;
        }
        else if (img.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            type = CvType.CV_8UC4;
        }
        else return new Mat();
        Mat m = new Mat(img.getHeight(), img.getWidth(), type);
        byte[] data =
                ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        if (type == CvType.CV_8UC1 || type == CvType.CV_8UC3) {
            m.put(0, 0, data);
            return m;
        }
        byte[] buf = Arrays.copyOf(data, data.length);
        byte tmp;
        for (int i = 0; i < buf.length; i += 4) { // ABGR => BGRA
            tmp = buf[i];
            buf[i] = buf[i + 1];
            buf[i + 1] = buf[i + 2];
            buf[i + 2] = buf[i + 3];
            buf[i + 3] = tmp;
        }
        m.put(0, 0, buf);
        return m;
    }
}
