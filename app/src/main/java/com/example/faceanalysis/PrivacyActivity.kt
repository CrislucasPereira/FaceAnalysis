package com.example.faceanalysis

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.TextView

class PrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        // Configura Toolbar com botão de voltar
        val toolbar = findViewById<Toolbar>(R.id.privacyToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Exibe o texto
        val tvPrivacyText = findViewById<TextView>(R.id.tvPrivacyText)
        tvPrivacyText.text = """
📄 TERMOS DE USO – DriveOn

Última atualização: 08/10/2025

Bem-vindo ao DriveOn, um aplicativo desenvolvido pela Equipe do Projeto TCC, com o objetivo de auxiliar na detecção de fadiga em motoristas por meio de análise facial com Inteligência Artificial.

Ao utilizar o DriveOn, o usuário concorda com os presentes Termos de Uso. Caso não concorde com alguma condição, recomenda-se não utilizar o aplicativo.

1. OBJETIVO DO APLICATIVO

O DriveOn tem finalidade exclusivamente acadêmica e experimental, voltada à pesquisa e demonstração tecnológica.
Ele realiza a captura e análise de imagens faciais e sons ambientais para identificar sinais de cansaço e sonolência, oferecendo feedbacks visuais e/ou sonoros.

2. ACEITE DOS TERMOS

O uso do aplicativo implica o aceite integral e irrestrito destes Termos.
Ao se cadastrar, o usuário também aceita a Política de Privacidade, que trata da coleta e uso de dados pessoais conforme a Lei nº 13.709/2018 (LGPD).

3. CADASTRO E CONTA DO USUÁRIO

Para utilizar o DriveOn, é necessário fornecer nome e e-mail válidos.
O usuário se compromete a fornecer informações verídicas e manter sua conta em segurança.
O uso de contas falsas, de terceiros ou com dados incorretos poderá resultar na exclusão imediata.

4. FUNCIONALIDADES PRINCIPAIS

Captura de imagens da câmera para análise facial.

Coleta de áudio ambiente para identificação de sons indicativos de fadiga.

Processamento dos dados utilizando MediaPipe, ONNX Runtime e Firebase.

Armazenamento de informações localmente e em nuvem (Firebase).

5. USO ADEQUADO

O aplicativo deve ser utilizado somente para fins pessoais e de pesquisa.
É proibido:

Utilizar o app para fins comerciais sem autorização;

Modificar, distribuir ou explorar o software indevidamente;

Utilizar o app para qualquer propósito ilegal, discriminatório ou que viole direitos de terceiros.

6. DIREITOS AUTORAIS

Todo o conteúdo, interface, logotipos e código-fonte do DriveOn pertencem à Equipe do Projeto TCC e estão protegidos pelas leis de propriedade intelectual vigentes.

7. RESPONSABILIDADE E LIMITAÇÕES

O DriveOn não substitui avaliação médica ou psicológica.
Os resultados apresentados têm caráter indicativo e experimental.
A Equipe do Projeto TCC não se responsabiliza por decisões tomadas com base nos resultados do aplicativo.

8. PRIVACIDADE E PROTEÇÃO DE DADOS

A coleta e o tratamento de dados seguem as disposições da Política de Privacidade do DriveOn, conforme a LGPD.
O usuário pode solicitar a exclusão de seus dados a qualquer momento pelo e-mail: cris9576654@gmail.com
.

9. ALTERAÇÕES DOS TERMOS

A Equipe do Projeto TCC poderá atualizar estes Termos periodicamente.
Recomenda-se que o usuário consulte esta página regularmente para estar ciente das alterações.

10. CONTATO

Dúvidas, sugestões ou solicitações relacionadas a este Termo podem ser enviadas para:
📩 cris9576654@gmail.com

🔒 POLÍTICA DE PRIVACIDADE – DriveOn

Última atualização: 08/10/2025

Esta Política de Privacidade descreve como o DriveOn coleta, utiliza e protege as informações pessoais dos usuários, em conformidade com a Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018 – LGPD).

1. CONTROLADOR DOS DADOS

Equipe do Projeto TCC
📩 cris9576654@gmail.com

Responsável por determinar as finalidades e os meios de tratamento dos dados pessoais coletados pelo aplicativo.

2. DADOS COLETADOS

O DriveOn coleta os seguintes dados pessoais:

Nome e e-mail: fornecidos no cadastro para identificação e autenticação.

Imagem facial (via câmera): usada para detecção de fadiga e expressões.

Áudio ambiente: utilizado apenas para detecção de sons relacionados à fadiga.

Dados técnicos: como versão do app e tipo de dispositivo (para fins de diagnóstico técnico).

3. FINALIDADE DO TRATAMENTO

Os dados são utilizados para:

Realizar análises faciais e sonoras visando detectar sinais de fadiga;

Melhorar a precisão dos algoritmos de reconhecimento;

Garantir a segurança da conta e autenticação do usuário;

Armazenar resultados e estatísticas no Firebase, de forma associada ao usuário.

4. BASE LEGAL

O tratamento dos dados é realizado com base no consentimento do titular, conforme o art. 7º, inciso I da LGPD.
O consentimento é obtido de forma expressa no momento do cadastro ou primeiro uso.

5. ARMAZENAMENTO DOS DADOS

Os dados podem ser armazenados:

Localmente no dispositivo do usuário, e

Em servidores Firebase (Google Cloud Platform), sob políticas de segurança e criptografia reconhecidas internacionalmente.

Os dados serão mantidos apenas pelo tempo necessário às finalidades do projeto acadêmico.

6. COMPARTILHAMENTO DE DADOS

Os dados não são compartilhados com terceiros, exceto:

Provedores de infraestrutura tecnológica (ex: Google/Firebase);

Autoridades legais, mediante solicitação formal e dentro da lei.

7. DIREITOS DO TITULAR

De acordo com a LGPD, o usuário tem direito a:

Confirmar a existência de tratamento;

Acessar seus dados pessoais;

Corrigir dados incompletos, inexatos ou desatualizados;

Solicitar a exclusão de dados;

Revogar o consentimento a qualquer momento.

Para exercer seus direitos, o usuário pode contatar: cris9576654@gmail.com

8. SEGURANÇA DA INFORMAÇÃO

O DriveOn adota medidas técnicas e administrativas para proteger os dados contra acesso não autorizado, perda, destruição ou alteração, incluindo:

Conexões seguras (HTTPS);

Criptografia no Firebase;

Controle de acesso restrito a membros do projeto.

9. ALTERAÇÕES NA POLÍTICA

Esta Política poderá ser alterada a qualquer momento, mediante publicação da nova versão no aplicativo.
O uso contínuo do app após alterações implica concordância com os novos termos.

10. CONTATO

Em caso de dúvidas sobre esta Política de Privacidade, entre em contato com:
📩 cris9576654@gmail.com
        """.trimIndent()
    }
}
