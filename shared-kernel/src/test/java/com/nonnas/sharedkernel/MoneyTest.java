package com.nonnas.sharedkernel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Nested
    @DisplayName("construção")
    class Construction {

        @Test
        @DisplayName("Money.brl com BigDecimal aplica escala 4 e moeda BRL")
        void brlFromBigDecimal() {
            Money m = Money.brl(new BigDecimal("10.5"));
            assertThat(m.amount()).isEqualByComparingTo("10.5000");
            assertThat(m.amount().scale()).isEqualTo(Money.UNIT_SCALE);
            assertThat(m.currency()).isEqualTo(Money.BRL);
        }

        @Test
        @DisplayName("Money.brl com String parse e aplica escala")
        void brlFromString() {
            assertThat(Money.brl("3.14").amount()).isEqualByComparingTo("3.14");
        }

        @Test
        @DisplayName("Money.brl com long converte para BigDecimal")
        void brlFromLong() {
            assertThat(Money.brl(42L).amount()).isEqualByComparingTo("42");
        }

        @Test
        @DisplayName("Money.zeroBrl é zero em BRL")
        void zeroBrl() {
            Money zero = Money.zeroBrl();
            assertThat(zero.isZero()).isTrue();
            assertThat(zero.currency()).isEqualTo(Money.BRL);
        }

        @Test
        @DisplayName("Money.zero aceita moeda customizada")
        void zeroWithCurrency() {
            assertThat(Money.zero(USD).currency()).isEqualTo(USD);
        }

        @Test
        @DisplayName("amount nulo lança NullPointerException")
        void nullAmountRejected() {
            assertThatThrownBy(() -> new Money(null, Money.BRL))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("currency nula lança NullPointerException")
        void nullCurrencyRejected() {
            assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("aritmética")
    class Arithmetic {

        @Test
        @DisplayName("add soma valores na mesma moeda")
        void addSameCurrency() {
            Money result = Money.brl("10.00").add(Money.brl("5.50"));
            assertThat(result.amount()).isEqualByComparingTo("15.5000");
        }

        @Test
        @DisplayName("subtract subtrai valores na mesma moeda")
        void subtractSameCurrency() {
            Money result = Money.brl("10.00").subtract(Money.brl("3.25"));
            assertThat(result.amount()).isEqualByComparingTo("6.7500");
        }

        @Test
        @DisplayName("multiply aplica fator e mantém escala 4")
        void multiplyKeepsScale() {
            Money result = Money.brl("2.00").multiply(new BigDecimal("3.5"));
            assertThat(result.amount()).isEqualByComparingTo("7.0000");
        }

        @Test
        @DisplayName("divide retorna escala 4 com HALF_EVEN")
        void divideUsesHalfEven() {
            Money result = Money.brl("10.00").divide(new BigDecimal("3"));
            assertThat(result.amount()).isEqualByComparingTo("3.3333");
        }

        @Test
        @DisplayName("divisão por zero lança ArithmeticException")
        void divideByZeroRejected() {
            assertThatThrownBy(() -> Money.brl("1.00").divide(BigDecimal.ZERO))
                    .isInstanceOf(ArithmeticException.class)
                    .hasMessage("Division by zero");
        }

        @Test
        @DisplayName("operação entre moedas diferentes lança IllegalArgumentException")
        void mixedCurrenciesRejected() {
            Money brl = Money.brl("1.00");
            Money usd = Money.zero(USD);
            assertThatThrownBy(() -> brl.add(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different currencies");
        }

        @Test
        @DisplayName("subtract entre moedas diferentes lança IllegalArgumentException")
        void subtractMixedCurrenciesRejected() {
            Money brl = Money.brl("1.00");
            Money usd = Money.zero(USD);
            assertThatThrownBy(() -> brl.subtract(usd))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("multiply com fator nulo lança NullPointerException")
        void multiplyNullFactorRejected() {
            assertThatThrownBy(() -> Money.brl("1.00").multiply(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("divide com divisor nulo lança NullPointerException")
        void divideNullDivisorRejected() {
            assertThatThrownBy(() -> Money.brl("1.00").divide(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("add com null lança NullPointerException")
        void addNullRejected() {
            assertThatThrownBy(() -> Money.brl("1.00").add(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("predicados de sinal")
    class Sign {

        @Test
        void positive() {
            assertThat(Money.brl("1.00").isPositive()).isTrue();
            assertThat(Money.brl("1.00").isNegative()).isFalse();
            assertThat(Money.brl("1.00").isZero()).isFalse();
        }

        @Test
        void negative() {
            assertThat(Money.brl("-1.00").isNegative()).isTrue();
            assertThat(Money.brl("-1.00").isPositive()).isFalse();
        }

        @Test
        void zero() {
            assertThat(Money.zeroBrl().isZero()).isTrue();
        }
    }

    @Nested
    @DisplayName("asTotal e display scale")
    class Display {

        @Test
        @DisplayName("asTotal arredonda para 2 casas com HALF_EVEN")
        void asTotalRoundsToTwoDecimals() {
            Money divided = Money.brl("10.00").divide(new BigDecimal("3")).asTotal();
            assertThat(divided.amount()).isEqualByComparingTo("3.33");
            assertThat(divided.amount().scale()).isEqualTo(Money.TOTAL_SCALE);
        }

        @Test
        @DisplayName("HALF_EVEN: 0.125 → 0.12 (banker's rounding)")
        void halfEvenBehavior() {
            Money half = Money.brl(new BigDecimal("0.125")).asTotal();
            assertThat(half.amount()).isEqualByComparingTo("0.12");
        }
    }

    @Nested
    @DisplayName("equality e hashCode")
    class Equality {

        @Test
        void equalsBasedOnAmountAndCurrency() {
            Money a = Money.brl("10.00");
            Money b = Money.brl("10.00");
            Money c = Money.brl("11.00");
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
            assertThat(a).isNotEqualTo(c);
        }
    }
}
