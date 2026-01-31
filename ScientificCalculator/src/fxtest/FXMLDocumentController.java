package fxtest;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class FXMLDocumentController implements Initializable {

    @FXML
    private TextArea historyDisplay;
    @FXML
    private TextField display;
    private double lastAnswer = 0;
    private List<String> historyList = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) { //@author Berk Haznedar
        display.setEditable(true);
        display.setFocusTraversable(true);
        display.requestFocus();
        display.setStyle("-fx-text-fill: black; -fx-background-color: white; "
                + "-fx-highlight-fill: #cde6ff; -fx-highlight-text-fill: black;");
        display.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode c = e.getCode();
            switch (c) {
                case LEFT:
                    moveLeft();
                    e.consume();
                    break;
                case RIGHT:
                    moveRight();
                    e.consume();
                    break;
                case DELETE:
                    deleteAtCursor();
                    e.consume();
                    break;
                case BACK_SPACE:
                    int p = display.getCaretPosition();
                    if (p > 0) {
                        display.deleteText(p - 1, p);
                    }
                    e.consume();
                    break;
                case HOME:
                    display.positionCaret(0);
                    e.consume();
                    break;
                case END:
                    display.positionCaret(display.getText().length());
                    e.consume();
                    break;
                default:
            }
        });
        display.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            e.consume();
        });
    }

    @FXML
    private void insertText(ActionEvent event) { //@author Berk Haznedar
        if (display.getText().length() >= 30) {
            return;
        }
        try {
            Button b = (Button) event.getSource();
            String text = b.getText();
            if (text.matches("sin|cos|tan|log|ln|sqrt")) {
                text += "(";
            } else if (text.equals("X^2")) {
                text = "^2";
            } else if (text.equals("X^Y")) {
                text = "^";
            }
            display.requestFocus();
            int p = display.getCaretPosition();
            display.selectRange(p, p);
            display.insertText(p, text);
            display.positionCaret(p + text.length());
        } catch (Exception e) {
            display.setText("Input Error");
        }
    }

    @FXML
    private void insertAnswer() { //@author Berk Haznedar
        if (display.getText().length() >= 30) {
            return;
        }
        String ansStr = (lastAnswer == (long) lastAnswer) ? String.format("%d", (long) lastAnswer) : String.valueOf(lastAnswer);
        display.requestFocus();
        int p = display.getCaretPosition();
        display.selectRange(p, p);
        display.insertText(p, ansStr);
        display.positionCaret(p + ansStr.length());
    }

    @FXML
    private void clearAll() { //@author Berk Haznedar
        display.clear();
        historyDisplay.clear();
        historyList.clear();
        display.requestFocus();
    }

    @FXML
    private void deleteAtCursor() { //@author Berk Haznedar
        display.requestFocus();
        int cursorPos = display.getCaretPosition();
        if (display.getSelectedText().isEmpty() && cursorPos > 0) {
            display.deleteText(cursorPos - 1, cursorPos);
        } else if (!display.getSelectedText().isEmpty()) {
            display.replaceSelection("");
        }
    }

    @FXML
    private void moveLeft() { //@author Berk Haznedar 
        int p = display.getCaretPosition();
        display.selectRange(p, p);
        if (p > 0) {
            display.positionCaret(p - 1);
        }
        display.requestFocus();
    }

    @FXML
    private void moveRight() { //@author Berk Haznedar
        int p = display.getCaretPosition();
        display.selectRange(p, p);
        if (p < display.getText().length()) {
            display.positionCaret(p + 1);
        }
        display.requestFocus();
    }

    @FXML
    private void calculate() { //@author Sertac Cakir, Berk Haznedar
        String expr = display.getText().trim();
        if (expr.isEmpty()) {
            return;
        }
        try {
            double result = evaluateExpression(expr);
            if (Double.isInfinite(result) || Double.isNaN(result)) {
                throw new ArithmeticException("Math Error");
            }
            lastAnswer = result;
            String resultStr = (result == (long) result) ? String.format("%d", (long) result) : String.valueOf(result);
            String historyEntry = expr + " = " + resultStr;
            historyList.add(historyEntry);
            updateHistoryDisplay();
            display.setText(resultStr);
            display.positionCaret(resultStr.length());
            display.requestFocus();
        } catch (Exception e) {
            display.setText("Syntax Error");
        }
    }

    private void updateHistoryDisplay() {  //@author Sertac Cakir
        StringBuilder historyText = new StringBuilder();
        // Geçmişi ters sırada ekleyerek en sonuncuyu en üste al
        for (int i = historyList.size() - 1; i >= 0; i--) {
            historyText.append(historyList.get(i)).append("\n");
        }
        historyDisplay.setText(historyText.toString());
        // Scroll'u en üste ayarla
        historyDisplay.setScrollTop(0);
    }

    private double evaluateExpression(String expression) { //@author Sertac Cakir
        List<String> tokens = tokenize(expression);
        List<String> postfix = infixToPostfix(tokens);
        return evaluatePostfix(postfix);
    }

    private List<String> tokenize(String expression) { //@author Sertac Cakir
        List<String> tokens = new ArrayList<>();
        // Regex 'e' ve 'π'yi ayrı token olarak yakalar
        Pattern pattern = Pattern.compile("([0-9]*\\.?[0-9]+)|(sin|cos|tan|log|ln|sqrt|e|π)|([\\+\\-\\*/x\\^\\(\\)])");
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private boolean isNumeric(String str) { //@author Sertac Cakir
        if (str == null) {
            return false;
        }
        // Sadece pozitif sayıları tanır
        return str.matches("([0-9]*\\.?[0-9]+)|([0-9]+\\.?[0-9]*)");
    }

    private boolean isFunction(String str) { //@author Sertac Cakir
        return str != null && str.matches("sin|cos|tan|log|ln|sqrt");
    }

    private boolean isConstant(String str) { //@author Sertac Cakir
        // Sabitler: e ve π (derece için π=180 olacak)
        return str != null && str.matches("e|π");
    }

    private int precedence(String op) { //@author Sertac Cakir
        //onem derecesi veya oncelik
        if (op == null) {
            return 0;
        }
        if (isFunction(op)) {
            return 5; // 1. Fonksiyonlar
        }
        switch (op) {
            case "^":
                return 4;     // 2. Üs alma
            case "_":
                return 3;     // 3. Unary minus (Negatif sayı yapma)
            case "*":
            case "/":
            case "x":
                return 2;     // 4. Çarpma/Bölme
            case "+":
            case "-":
                return 1;     // 5. Binary (Çıkarma)
            default:
                return 0;
        }
    }

    private List<String> infixToPostfix(List<String> tokens) { //@author Sertac Cakir, Berk Haznedar
        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();
        boolean expectingOperand = true; // Başlangıçta sayı beklenir

        for (String token : tokens) {

            // constant factor (baskatsayi)
            if (!expectingOperand && (isNumeric(token) || isConstant(token) || isFunction(token) || token.equals("("))) {
                String op = "*";
                while (!operators.isEmpty() && !operators.peek().equals("(")
                        && (precedence(operators.peek()) > precedence(op)
                        || (precedence(operators.peek()) == precedence(op) && !op.equals("^") && !op.equals("_")))) {
                    output.add(operators.pop());
                }
                operators.push(op);
                expectingOperand = true;
            }

            // process tokens
            if (isNumeric(token) || isConstant(token)) {
                output.add(token);
                expectingOperand = false;
            } else if (isFunction(token)) {
                operators.push(token);
                expectingOperand = true;
            } else if (token.equals("-") && expectingOperand) {
                operators.push("_"); // Unary minus
                expectingOperand = true;
            } else if (token.equals("(")) {
                operators.push(token);
                expectingOperand = true;
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }
                operators.pop(); // '(' pop the paranthese
                if (!operators.isEmpty() && isFunction(operators.peek())) {
                    output.add(operators.pop());
                }
                expectingOperand = false;
            } else {
                // Token is a normal binary operator (+, -, *, /, ^)
                while (!operators.isEmpty() && !operators.peek().equals("(")
                        && (precedence(operators.peek()) > precedence(token)
                        || (precedence(operators.peek()) == precedence(token) && !token.equals("^") && !token.equals("_")))) {
                    output.add(operators.pop());
                }
                operators.push(token);
                expectingOperand = true;
            }
        }

        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }
        return output;
    }

    private double snapTrigResult(double result) { // @author Sertac Cakir
        // sayıları 0, 1 ve -1 ' e yuvarla kolaylık icin
        double epsilon = 1E-15;
        if (Math.abs(result) < epsilon) {
            return 0.0;
        }
        if (Math.abs(result - 1.0) < epsilon) {
            return 1.0;
        }
        if (Math.abs(result + 1.0) < epsilon) {
            return -1.0;
        }
        return result; // Yakın değilse orijinali döndür
    }

    private double evaluatePostfix(List<String> postfixTokens) { //@author Sertac Cakir, Berk Haznedar
        Stack<Double> stack = new Stack<>();

        for (String token : postfixTokens) {

            if (isNumeric(token)) {
                stack.push(Double.parseDouble(token));
            } else if (isConstant(token)) {
                if (token.equals("π")) {
                    stack.push(180.0); // π = 180 dereceye cevir
                } else if (token.equals("e")) {
                    stack.push(Math.E); // e = 2.718... olarak al
                }
            } else if (isFunction(token)) {
                // Fonksiyonlar 1 sayı alir (derece  modu)
                double val = stack.pop();
                double result; // Hesaplama sonucunu tut

                switch (token) {
                    // Math.toRadians() for degree (derece hesapla)
                    // snapTrigResult kullanarak sonucu sadeleştir
                    case "sin":
                        result = Math.sin(Math.toRadians(val));
                        stack.push(snapTrigResult(result));
                        break;
                    case "cos":
                        result = Math.cos(Math.toRadians(val));
                        stack.push(snapTrigResult(result));
                        break;
                    case "tan":
                        result = Math.tan(Math.toRadians(val));
                        stack.push(snapTrigResult(result));
                        break;
                    // Diğer fonksiyonlar bagimsizdir (derece/radyan kullanılmaz)
                    case "log":
                        stack.push(Math.log10(val));
                        break;
                    case "ln":
                        stack.push(Math.log(val));
                        break;
                    case "sqrt":
                        stack.push(Math.sqrt(val));
                        break;
                }
            } else if (token.equals("_")) {
                // Unary minus (sayıyı negatif yapma),  1 sayı alir
                double val = stack.pop();
                stack.push(-val);
            } else {
                // Binary Operatörler (+, -, *, /), 2 sayı alir
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Error: Not enough operands for operator.");
                }
                double val2 = stack.pop();
                double val1 = stack.pop();
                switch (token) {
                    case "+":
                        stack.push(val1 + val2);
                        break;
                    case "-":
                        stack.push(val1 - val2);
                        break; // Normal çıkarma
                    case "*":
                    case "x":
                        stack.push(val1 * val2);
                        break;
                    case "/":
                        if (val2 == 0) {
                            throw new ArithmeticException("Error: Division by zero");
                        }
                        stack.push(val1 / val2);
                        break;
                    case "^":
                        stack.push(Math.pow(val1, val2));
                        break;
                }
            }
        }

        // result should be the only value in the stack
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Error: Invalid expression or stack error.");
        }
        return stack.pop();
    }
}
