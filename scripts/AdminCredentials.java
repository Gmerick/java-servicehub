import java.io.Console;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Properties;
import org.springframework.security.crypto.bcrypt.BCrypt;

/** Run locally with a JDK; never pass passwords as arguments or print the hash. */
class AdminCredentials {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Informe somente o caminho do arquivo de credenciais.");
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Execute em um terminal interativo local.");
        Path destination = Path.of(args[0]).toAbsolutePath();
        if (Files.exists(destination)) throw new IllegalStateException("O arquivo já existe; não será sobrescrito.");
        String username = console.readLine("Usuário administrador (3-80 letras/números/._-): ");
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,80}")) throw new IllegalArgumentException("Usuário inválido.");
        char[] password = console.readPassword("Senha administrativa (12 a 72 bytes UTF-8): ");
        char[] confirmation = console.readPassword("Repita a senha: ");
        try {
            if (password == null || confirmation == null || password.length < 12 || !Arrays.equals(password, confirmation)
                    || new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
                throw new IllegalArgumentException("Senha inválida ou confirmação divergente.");
            Properties properties = new Properties();
            properties.setProperty("app.admin.username", username);
            properties.setProperty("app.admin.password-hash", BCrypt.hashpw(new String(password), BCrypt.gensalt(12)));
            if (destination.getFileSystem().supportedFileAttributeViews().contains("posix"))
                Files.createFile(destination, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            else Files.createFile(destination);
            try (var out = Files.newOutputStream(destination)) { properties.store(out, "ServiceHub - manter fora do Git"); }
            console.printf("Credenciais gravadas em %s. Restrinja o acesso ao arquivo.\n", destination);
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}
