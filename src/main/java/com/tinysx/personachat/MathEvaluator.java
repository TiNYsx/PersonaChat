package com.tinysx.personachat;

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance Abstract Syntax Tree (AST) Math Evaluator for real-time animations.
 * Supports: +, -, *, /, %, ^, sqrt(), sin(), cos(), tan(), abs(), floor(), ceil(), round(), log(), min(), max(),
 * and dynamic variables:
 * - 'i' (stack index)
 * - 't' (server time in seconds)
 * - 'l' (message lifetime in seconds)
 * - 'r' (message random float [0.0, 1.0])
 */
public class MathEvaluator {

    @FunctionalInterface
    public interface Expression {
        double evaluate(double i, double t, double l, double r);
    }

    public static Expression compile(String exprStr) {
        if (exprStr == null || exprStr.trim().isEmpty()) {
            return (i, t, l, r) -> 0.0;
        }

        try {
            double constant = Double.parseDouble(exprStr.trim());
            return (i, t, l, r) -> constant;
        } catch (NumberFormatException ignored) {
        }

        try {
            Parser parser = new Parser(tokenize(exprStr));
            return parser.parseExpression();
        } catch (Exception e) {
            return (i, t, l, r) -> 0.0;
        }
    }

    private enum TokenType {
        NUMBER, VAR_I, VAR_T, VAR_L, VAR_R,
        PLUS, MINUS, MUL, DIV, MOD, POW,
        LPAREN, RPAREN, COMMA,
        FUNC_SQRT, FUNC_SIN, FUNC_COS, FUNC_TAN,
        FUNC_ABS, FUNC_FLOOR, FUNC_CEIL, FUNC_ROUND, FUNC_LOG,
        FUNC_MIN, FUNC_MAX
    }

    private static class Token {
        final TokenType type;
        final double numberValue;

        Token(TokenType type) {
            this(type, 0);
        }

        Token(TokenType type, double numberValue) {
            this.type = type;
            this.numberValue = numberValue;
        }
    }

    private static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int len = input.length();
        int idx = 0;

        while (idx < len) {
            char c = input.charAt(idx);

            if (Character.isWhitespace(c)) {
                idx++;
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                int start = idx;
                while (idx < len && (Character.isDigit(input.charAt(idx)) || input.charAt(idx) == '.')) {
                    idx++;
                }
                double val = Double.parseDouble(input.substring(start, idx));
                tokens.add(new Token(TokenType.NUMBER, val));
                continue;
            }

            if (Character.isLetter(c)) {
                int start = idx;
                while (idx < len && Character.isLetter(input.charAt(idx))) {
                    idx++;
                }
                String word = input.substring(start, idx).toLowerCase();

                switch (word) {
                    case "i" -> tokens.add(new Token(TokenType.VAR_I));
                    case "t" -> tokens.add(new Token(TokenType.VAR_T));
                    case "l" -> tokens.add(new Token(TokenType.VAR_L));
                    case "r" -> tokens.add(new Token(TokenType.VAR_R));
                    case "pi" -> tokens.add(new Token(TokenType.NUMBER, Math.PI));
                    case "e" -> tokens.add(new Token(TokenType.NUMBER, Math.E));
                    case "sqrt" -> tokens.add(new Token(TokenType.FUNC_SQRT));
                    case "sin" -> tokens.add(new Token(TokenType.FUNC_SIN));
                    case "cos" -> tokens.add(new Token(TokenType.FUNC_COS));
                    case "tan" -> tokens.add(new Token(TokenType.FUNC_TAN));
                    case "abs" -> tokens.add(new Token(TokenType.FUNC_ABS));
                    case "floor" -> tokens.add(new Token(TokenType.FUNC_FLOOR));
                    case "ceil" -> tokens.add(new Token(TokenType.FUNC_CEIL));
                    case "round" -> tokens.add(new Token(TokenType.FUNC_ROUND));
                    case "log" -> tokens.add(new Token(TokenType.FUNC_LOG));
                    case "min" -> tokens.add(new Token(TokenType.FUNC_MIN));
                    case "max" -> tokens.add(new Token(TokenType.FUNC_MAX));
                    default -> tokens.add(new Token(TokenType.NUMBER, 0));
                }
                continue;
            }

            switch (c) {
                case '+' -> tokens.add(new Token(TokenType.PLUS));
                case '-' -> tokens.add(new Token(TokenType.MINUS));
                case '*' -> tokens.add(new Token(TokenType.MUL));
                case '/' -> tokens.add(new Token(TokenType.DIV));
                case '%' -> tokens.add(new Token(TokenType.MOD));
                case '^' -> tokens.add(new Token(TokenType.POW));
                case '(' -> tokens.add(new Token(TokenType.LPAREN));
                case ')' -> tokens.add(new Token(TokenType.RPAREN));
                case ',' -> tokens.add(new Token(TokenType.COMMA));
            }
            idx++;
        }

        return tokens;
    }

    private static class Parser {
        private final List<Token> tokens;
        private int pos = 0;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        private Token peek() {
            if (pos < tokens.size()) return tokens.get(pos);
            return null;
        }

        private Token next() {
            if (pos < tokens.size()) return tokens.get(pos++);
            return null;
        }

        Expression parseExpression() {
            Expression left = parseTerm();

            while (peek() != null && (peek().type == TokenType.PLUS || peek().type == TokenType.MINUS)) {
                TokenType op = next().type;
                Expression right = parseTerm();
                Expression prevLeft = left;

                if (op == TokenType.PLUS) {
                    left = (i, t, l, r) -> prevLeft.evaluate(i, t, l, r) + right.evaluate(i, t, l, r);
                } else {
                    left = (i, t, l, r) -> prevLeft.evaluate(i, t, l, r) - right.evaluate(i, t, l, r);
                }
            }

            return left;
        }

        private Expression parseTerm() {
            Expression left = parseFactor();

            while (peek() != null && (peek().type == TokenType.MUL || peek().type == TokenType.DIV || peek().type == TokenType.MOD)) {
                TokenType op = next().type;
                Expression right = parseFactor();
                Expression prevLeft = left;

                if (op == TokenType.MUL) {
                    left = (i, t, l, r) -> prevLeft.evaluate(i, t, l, r) * right.evaluate(i, t, l, r);
                } else if (op == TokenType.DIV) {
                    left = (i, t, l, r) -> {
                        double divisor = right.evaluate(i, t, l, r);
                        return divisor == 0 ? 0 : prevLeft.evaluate(i, t, l, r) / divisor;
                    };
                } else {
                    left = (i, t, l, r) -> {
                        double divisor = right.evaluate(i, t, l, r);
                        return divisor == 0 ? 0 : prevLeft.evaluate(i, t, l, r) % divisor;
                    };
                }
            }

            return left;
        }

        private Expression parseFactor() {
            Expression left = parsePrimary();

            while (peek() != null && peek().type == TokenType.POW) {
                next();
                Expression right = parseFactor();
                Expression prevLeft = left;
                left = (i, t, l, r) -> Math.pow(prevLeft.evaluate(i, t, l, r), right.evaluate(i, t, l, r));
            }

            return left;
        }

        private Expression parsePrimary() {
            Token token = next();
            if (token == null) return (i, t, l, r) -> 0.0;

            if (token.type == TokenType.MINUS) {
                Expression inner = parsePrimary();
                return (i, t, l, r) -> -inner.evaluate(i, t, l, r);
            }

            if (token.type == TokenType.PLUS) {
                return parsePrimary();
            }

            if (token.type == TokenType.NUMBER) {
                double val = token.numberValue;
                return (i, t, l, r) -> val;
            }

            if (token.type == TokenType.VAR_I) return (i, t, l, r) -> i;
            if (token.type == TokenType.VAR_T) return (i, t, l, r) -> t;
            if (token.type == TokenType.VAR_L) return (i, t, l, r) -> l;
            if (token.type == TokenType.VAR_R) return (i, t, l, r) -> r;

            if (token.type == TokenType.LPAREN) {
                Expression inner = parseExpression();
                if (peek() != null && peek().type == TokenType.RPAREN) next();
                return inner;
            }

            if (token.type == TokenType.FUNC_SQRT || token.type == TokenType.FUNC_SIN || token.type == TokenType.FUNC_COS ||
                token.type == TokenType.FUNC_TAN || token.type == TokenType.FUNC_ABS || token.type == TokenType.FUNC_FLOOR ||
                token.type == TokenType.FUNC_CEIL || token.type == TokenType.FUNC_ROUND || token.type == TokenType.FUNC_LOG) {
                
                TokenType fType = token.type;
                if (peek() != null && peek().type == TokenType.LPAREN) next();
                Expression arg = parseExpression();
                if (peek() != null && peek().type == TokenType.RPAREN) next();

                return switch (fType) {
                    case FUNC_SQRT -> (i, t, l, r) -> Math.sqrt(Math.max(0, arg.evaluate(i, t, l, r)));
                    case FUNC_SIN -> (i, t, l, r) -> Math.sin(arg.evaluate(i, t, l, r));
                    case FUNC_COS -> (i, t, l, r) -> Math.cos(arg.evaluate(i, t, l, r));
                    case FUNC_TAN -> (i, t, l, r) -> Math.tan(arg.evaluate(i, t, l, r));
                    case FUNC_ABS -> (i, t, l, r) -> Math.abs(arg.evaluate(i, t, l, r));
                    case FUNC_FLOOR -> (i, t, l, r) -> Math.floor(arg.evaluate(i, t, l, r));
                    case FUNC_CEIL -> (i, t, l, r) -> Math.ceil(arg.evaluate(i, t, l, r));
                    case FUNC_ROUND -> (i, t, l, r) -> (double) Math.round(arg.evaluate(i, t, l, r));
                    case FUNC_LOG -> (i, t, l, r) -> Math.log(Math.max(0.0001, arg.evaluate(i, t, l, r)));
                    default -> (i, t, l, r) -> 0.0;
                };
            }

            if (token.type == TokenType.FUNC_MIN || token.type == TokenType.FUNC_MAX) {
                TokenType fType = token.type;
                if (peek() != null && peek().type == TokenType.LPAREN) next();
                Expression arg1 = parseExpression();
                if (peek() != null && peek().type == TokenType.COMMA) next();
                Expression arg2 = parseExpression();
                if (peek() != null && peek().type == TokenType.RPAREN) next();

                if (fType == TokenType.FUNC_MIN) {
                    return (i, t, l, r) -> Math.min(arg1.evaluate(i, t, l, r), arg2.evaluate(i, t, l, r));
                } else {
                    return (i, t, l, r) -> Math.max(arg1.evaluate(i, t, l, r), arg2.evaluate(i, t, l, r));
                }
            }

            return (i, t, l, r) -> 0.0;
        }
    }
}
