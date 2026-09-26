package lucns.heartratemonitor.data;

public class Stabilizer {

    private static final int SIZE = 512;
    private int[] buffer = new int[SIZE];
    private int index = 0;
    private int count = 0;
    private int sum = 0;

    public Stabilizer() {}

    public Stabilizer(int[] values) {
        buffer = values;
    }

    public int getMinimumAverage() {
        return 0;
    }

    public int put(int value) {
        if (count == SIZE) {
            sum -= buffer[index];
        } else {
            count++;
        }

        buffer[index] = value;
        sum += value;

        index = (index + 1) % SIZE;
        return sum / count;
    }

    public static int[] filterNoise(int[] amplitudes, int tamanhoJanela) {
        if (amplitudes == null || amplitudes.length == 0) {
            return new int[0];
        }
        if (tamanhoJanela < 2) return amplitudes;

        int tamanho = amplitudes.length;
        int[] sinalFiltrado = new int[tamanho];

        int raio = tamanhoJanela / 2;
        for (int i = 0; i < tamanho; i++) {
            long soma = 0;
            int contador = 0;

            int inicio = Math.max(0, i - raio);
            int fim = Math.min(tamanho - 1, i + raio);

            for (int j = inicio; j <= fim; j++) {
                soma += amplitudes[j];
                contador++;
            }
            sinalFiltrado[i] = (int) (soma / contador);
        }
        return sinalFiltrado;
    }
}
