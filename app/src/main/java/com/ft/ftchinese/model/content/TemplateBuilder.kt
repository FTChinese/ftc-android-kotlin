package com.ft.ftchinese.model.content
import android.util.Log
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.tracking.AdParser
import com.ft.ftchinese.tracking.AdPosition
import com.ft.ftchinese.tracking.JSCodes

private const val TAG = "StoryBuilder"


// MARK: - Go to the token repository and run "node update-author-headshot.js" to get updated headshots
private val headshots = mapOf("ETHAN WU" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/3574d8748c91aaa598fd6a7f63aa2a9e.png","HANNAH MURPHY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/484641abf2379953062c23c2f96b9edf.png","ANDREW EDGECLIFFE-JOHNSON" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/c15f4f78b6a22d02535ca8fabd0b3baf.png","MOHAMED EL-ERIAN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/2ee87493479275b151d938153b479bc2.png","JONATHAN BLACK" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/bc78eaf16745c4678c10fcbc6dab8c31.png","SHOLA ASANTE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/38829bc43e39a5e445c7c203904f97bb.png","MARTIN SANDBU" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/b5df35deb5b57ea81eff9907e3a2cf70.png","VIV GROSKOP" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/7dcab9c39bbef8324fa49e36500aa4e3.png","GARY SILVERMAN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/f2f856f6c4ac42ba4d70fbb0932a0be8.png","CHRISTOPHER GRIMES" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/bc6b1b695dd0effe31b3b7caab7427a9.png","JANAN GANESH" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/a557fe74afcc6301b438cceda2da39bb.png","TONY BARBER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/92b9f97ae1b4c5b024aee9d3d413b15d.png","JO ELLISON" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/06adaf75a18041d343bccca97b595298.png","ELAINE MOORE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/c8ed012fa300b9a9e8aa909382ed260f.png","ROBIN LANE FOX" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/fdfc57e700c9d95dc4dc7ff40ddcabf6.png","CAMILLA CAVENDISH" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/8e30a9dd094ac1f7c88d5541998ef72e.png","KATIE MARTIN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/c3d6864eceb5abc7b75012d29e0ffb9e.png","SUJEET INDAP" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/e10472634ca1b6e57b241ed7d5a916e5.png","LUKE EDWARD HALL" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/9ebff384cd3f9294d6fb6c81d0471805.png","HELEN THOMAS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/4ee8668a91e2b37be6396ecea4f56612.png","RICHARD WATERS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/0943832545b7e93f8530d4a10347d802.png","GILLIAN TETT" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/9a0a1b47a3cbd06bcf81083b71eb6977.png","JOHN BURN-MURDOCH" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/8942dfec191efdf53c91a76449dedfaa.png","CHRIS GILES" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/d9c0ea1d609acf13a98c32dbf2b61e2e.png","TOM MITCHELL" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/1e90cd6571cca58a294c269a98042bd9.png","ROBERT ARMSTRONG" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/00d45fe3cd3299fcd116bb939dda8537.png","CLAER BARRETT" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/e32649880e75de6b01e44388b19094fb.png","MERRYN SOMERSET WEBB" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/f6bc054a14985005d8c3a68dd9988c57.png","JEMIMA KELLY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/a8c972fa3910926142db551980147864.png","STEPHEN BUSH" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/0d3058967a557ca48e78d8451b1d0356.png","SEBASTIAN PAYNE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/2152707c9d5a2d03e87c372c9eca0b71.png","VANESSA HOULDER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/d454b87d23ee419d0f38f11d7e871fe9.png","ALAN BEATTIE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/ca06405ece151c953ed7604914f27b0e.png","PEGGY HOLLINGER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/4a30b2c4df08650ba9ee52aa0a580ab6.png","LUCY WARWICK-CHING" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/c842b336c2dc7358ccc9b03289ff4875.png","CAT RUTTER POOLEY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/85f2eada918b13810b6c93185fe7294b.png","KATHRIN HILLE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/d679d95db2b68f651267f75c1987f2be.png","ANJANA AHUJA" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/32ab32fc452913f38ee53390f50f6f38.png","JENNIFER CREERY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/424fe6614e4888bce6f54a4f276f64ef.png","SARAH O'CONNOR" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/d900693bcc0ffca0ebb0931ccfb432ba.png","BRONWEN MADDOX" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/3c7eabfb45f1a949fc88c7bb010f37ab.png","JESSICA RAWNSLEY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/02f9d2a03ed1c209c6d91e6220a44d2f.png","MEGAN GREENE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/234fb265ac6ebfe35f18ba8b8e58ba73.png","RUCHIR SHARMA" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/bd49968a794512a6b65221facd417b38.png","PATRICK JENKINS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/b50e28382f30f3894988e96b74a78453.png","LYNDA GRATTON" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/5a2a99ab97f53565cd3231a37cb6681b.png","CONSTANZE STELZENMÜLLER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/166ca8d6b1c0261d229b4480f7dd298a.png","LEO LEWIS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/9d535c1eabab753190138743ee15b328.png","LESLIE HOOK" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/e4a540105b5f90c6fd4794c97ec1bffc.png","ENUMA OKORO" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/7e4297bb00854fad9894c0a4d10e3205.png","NILANJANA ROY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/9a6c4f89d89dfdbf255cb89662f6da22.png","ALICE FISHBURN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/87833a2a196a630820a19f15b5b0308c.png","JOE MILLER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/72843f6512de0de01d43dcb66ee4207c.png","JOHN GAPPER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/9c98779c8fe878df55ddee4d5d9b5c37.png","JOHN THORNHILL" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/2d7fe8aef305536a7ad8bcbfd7153b6b.png","LIONEL BARBER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/756f25af2b9fd2e86a7bd6f4a1977afc.png","PAN KWAN YUK" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/b0fafacdca9087d6221e68b42cc9b68b.png","PETER CAMPBELL" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/68abfc7b4633f5e786b3271da8fe5ed7.png","BROOKE MASTERS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/34c8cfcd1ee2bc5f26b9468356e653f8.png","RYAN MCMORROW" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/1031f9e4544208c15138281a5b893de2.png","LEILA ABBOUD" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/7f43844743878da7bc99e7f698b4ff37.png","MARTIN WOLF" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/9af4939391a1c9057eaecd62e93f3460.png","VICTOR MALLET" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/b89384d310114006f1c7ddb8852a0c65.png","JOHN REDWOOD" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/a8a9adfbd22bcf44ff1086fcc6fb9a7b.png","PATTI WALDMEIR" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/5c03fcfb0581d1e97489ad7aecb51974.png","OREN CASS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/db5daf2458cd0f4b9707fa16dd0ea5ab.png","GIDEON RACHMAN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/323c1b2250a205944ef034dcd6088d1e.png","RANA FOROOHAR" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/2ac500f74aafac9c9b3ec358cf595229.png","DAVID PILLING" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/8b60874f865f2d5a0f66bd639749f47f.png","MIRANDA GREEN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/00fca5013462dcec4769e808e352a981.png","REBECCA ROSE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/08728aff4e66497f0d87453bcf7ab86d.png","JONATHAN MOULES" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/949f6ceb32089f1450235836602657e4.png","BRYCE ELDER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/a878da7dec6ffe5f37d0ea91b0aab0eb.png","ANNA NICOLAOU" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/220244f9178110df9c1dbb1e1ba1f705.png","ROBERT SHRIMSLEY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/d6bd48d3824ac6e0514e5c88ad423b8e.png","DON EZRA" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/ea38d4daa034d07c0ea658566ca073a7.png","JONATHAN ELEY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/d8f366bf206f548bd1526ee4841c6a2e.png","EDWARD LUCE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/4d65778240804a96caa847ba60bc5b86.png","JOY LO DICO" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/75c8e306c1ee4bfe609be5fda473a5bb.png","GUY CHAZAN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/cde630b37086ff6c34a3fe6978f66b77.png","DAVID STEVENSON" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/8c8d818224a11a2301dadf168b28b553.png","ANNA BERKELEY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/17e335aef897f6a8304dcfbf197e9a0e.png","AMY KAZMIN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/259c196dfca122ea2b0067671949b514.png","PATRICK TEMPLE-WEST" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/69ad5e00a349c210d437ac688123f125.png","HENRY MANCE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/428ca07b21866ae5907f52ade2a67a57.png","FEDERICA COCCO" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/e47f8e486f482d3f128284741abbf6dd.png","EMMA JACOBS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/5cef09dc52db2ab55aa795161a4ff05e.png","OLAF STORBECK" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/3dc0c7623bb804aaa2bc9eae42741c3f.png","JUDE WEBBER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/d3045251b9102ad3a5ce77a6681a9816.png","MARK VANHOENACKER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/8861ce1b6a86965a0afc8801dc343bb2.png","BRYAN HARRIS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/c421a72b44919b3b898ed668e503c908.png","JONATHAN GUTHRIE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/418503d75f0b0c40128211a959aa4bb5.png","LUDOVIC HUNTER-TILNEY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/8b0f65bb3a3cf9be82bd447b1577d6ce.png","MOIRA O'NEILL" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/70af5774a385e70567622a93ddf53d44.png","JOSHUA CHAFFIN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/b016f1d5c833edfd1a48662ba6af4b62.png","JAMES FONTANELLA-KHAN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/98096596c36e9728f856cd00042e604a.png","JAMES MAX" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/394d5f079da5a34d42c8a168da3bb6ee.png","JUNE YOON" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/5e192fcc23b7ff06a2c09570e58370fd.png","MADHUMITA MURGIA" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/52f534036652bad5b58c6fc8fa4f33e7.png","HENRY FOY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/2ae7b6d5e39044304d20e5331570285e.png","BENJAMIN PARKIN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/5ad0f93898ee6ce4a458394a8c99b4aa.png","PILITA CLARK" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/a24b3b6560377cc6477c7b5efba7e100.png","CHAN HO-HIM" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/cac4fb6e1846fda01083dcabd8b13ad1.png","ANDREW HILL" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/7d92c090d9eeda1adf97c86b083cf611.png","BEN HALL" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/0688719e96a1cc8895888b9282b7f06c.png","BRENDAN GREELEY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/c3e81d7c353e8668b7d2517e7cbdcde4.png","IVAN KRASTEV" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/813e0933e0b8ddad6c844760efcecd88.png","LAURA NOONAN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/22c09b3b28808d43115283df1ac6da60.png","SILVIA SCIORILLI BORRELLI" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/cdef9c4a90e1de0fa87d3b58c30f6f46.png","SAM JONES" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/2fc25ae5e3c73513188605e9c45eff46.png","ELIZABETH UVIEBINENE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/36fcd6b830c3f36e89f8139f1e9f01b7.png","KANA INAGAKI" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/d8b8d08fd7efbed95719bb48346b22b7.png","ANNE-SYLVAINE CHASSANY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/6e905ba598be780d699b129e40606f40.png","JOHN PLENDER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/2dc76eb726c3dc29e45037af3fb34751.png","JAMES KYNGE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/41da56df0da19d6d94e2b95ee2d4d270.png","TOM BRAITHWAITE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/3ebbd34e1c252258907486ab4ecd908c.png","DAVID SHEPPARD" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/e2579fa4cd77b274a4cf8931e4009ed6.png","OLIVER ROEDER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/967d32939398fb65cb38a568ae96ae01.png","NAJMEH BOZORGMEHR" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/a81f1de812a2bb7eb6f455f014797e8f.png","VALENTINA ROMEI" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/df98f1cb77e1dfce090a73512eba8a3b.png","PAUL LEWIS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/ec371c94d370c9922f9bbdb15028cd8d.png","EMIKO TERAZONO" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/65261cd298a18a91e96c09a7924a2c0a.png","NIC FILDES" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/ac65ab95a0b953efd2c97b08edc233c7.png","LAURENCE FLETCHER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/b1058a3d8ef96441731d597a6108f8f8.png","JAN DALLEY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/706e8e65b8d8a960335172599a1a3d1a.png","MICHAEL STOTT" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/ac0900c528440f12cf5629e6b608f5b6.png","FREDERICK STUDEMANN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/6e781b76e8d191aff0288c526be23557.png","RICHARD MILNE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/9e2d7f0b4298663628006e8313bc0686.png","JOHN KAY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/4f2cd034fe986a7aec80225458c81436.png","ROBIN HARDING" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/889c64d0859edc036e5cebb2d73a8cc3.png","JOHN LEE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/ef284063a01b86abc00a706fabe8f1f6.png","CLAIRE JONES" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/4387fc58190ad5329f61ad8ba47ef8e9.png","MICHAEL POOLER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/c3d10d8d43418579adff8deb03c9a5f6.png","MAIKE CURRIE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/24002b93316319926ccb91f9c4284005.png","MARGARET HEFFERNAN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/fb16aad70e6e8b912296e4e73b479235.png","KATE BURGESS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/96d14c1d81894a88ddb09fc8555bf80a.png","ANDREW JACK" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/cb503946355ae32fe27ec54b6d972409.png","SIDDHARTH VENKATARAMAKRISHNAN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/bd7782f5778438c7837c61c61acbfd2c.png","NEAL HUDSON" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/b3d3f3013b25fbc5a0e93983563c3d97.png","JASON BUTLER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/1bc9435c2358f5e86a32c84da4635bb4.png","MARIETJE SCHAAKE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/5e262f43ec19c3593630621808f46513.png","ANTONI SLODKOWSKI" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/eec14c6354dd4cda0060b3f9ab134984.png","JANINE GIBSON" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/8d514e410fdf226d23c7815f3faa896a.png","LAUREN INDVIK" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/e37389364882b9ff30faa749fd19cf4e.png","BOBBY SEAGULL" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/e0170d343808803aa73986843072cefe.png","LAURA PITEL" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/b08a11d63bb320f739efa30bdc0a8838.png","JUNE ANGELIDES" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/05d57654e047396b48a9ac6c76bbacc0.png","CHLOE CORNISH" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/1cbdc758b93cec9c0af2ea4e8ade624f.png","NICHOLAS MEGAW" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/0e0f46fe23dccb127dfefcb88aa2e4f5.png","RAGHURAM RAJAN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/28bdf05f558fd2b6c35f0cb199fc27df.png","ANNE-MARIE SLAUGHTER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/f784d1dffb04820d7fba653a1c521c29.png","DAVE LEE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/e690df13483e1a4920067daf7f1c4ec5.png","CHRISTIAN DAVIES" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/af4c730cf0d57e94dd8ad84bbeffa129.png","TABBY KINDER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/12181cf8581189340864002e89e0ed43.png","VALENTINA POP" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/3fe0abe21c861c178ec6a1f453548ebd.png","ELI MEIXLER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/a45c0db2be471067579d1db09ddd2036.png","CLIVE COOKSON" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/7eaa17f8a099c0a622ab4bba6c4b6289.png","CHRIS COOK" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/2c092618fa340b418f5739463d4b61f2.png","MADISON DARBYSHIRE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/5ebd5088fb6a59de74811bf395922e8e.png","KADHIM SHUBBER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/2f051e92cc51c7121cbad4aa97f18fc8.png","SUSIE BOYT" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/8804ea669a2e5f149f8fc5b84a4ad607.png","EMMA BOYDE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/4e4bc1a77db81d342c4e40a0e8fb3be9.png","TIM BRADSHAW" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/d0b438505b4b1d0d10d399dfcf532d37.png","DEREK BROWER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/6994d93d401b863a7218b96e3a15515d.png","LEYLA BOULTON" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/949c22ba05798b4930313fa170b9e799.png","ANDREW SMITHERS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/31513e0a4837b39db946be7f277ba0ad.png","DAVID GARDNER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/a135210fc92b62d925321896cf189cba.png","PHILIP STAFFORD" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/8254e2369d6b8dd326999377dffb3f00.png","ALAN LIVSEY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/4db4de7466c6a76a59afd24ab8b92f8b.png","HUGO COX" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/bd93c5b55bfc54acf8bdc93a705b1955.png","KATE BEIOLEY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/df2997f672c318443fc5d31e59891cf6.png","TOM ROBBINS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/cfaf55b8fc9d51809f5ec7561ffb8711.png","ROBIN WIGGLESWORTH" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/6e1a69f9aa39bac86a71895b3226137a.png","RAVI MATTU" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/c59942fd10538cfb0fecbaec7ca86377.png","LUCY KELLAWAY" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/c676803918e0b92cc0d07f1b06cdfbdd.png","FRANKLIN NELSON" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/45dd8e555c1ce1e8475601dac67d0f9c.png","PRIMROSE RIORDAN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/5dc1619312908f8cf55a3f47fef84235.png","JOSEPH COTTERILL" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/e48ac591bd13fcd3b7cc159f2790cae1.png","CAROLA LONG" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/13d9554afa9eccd20aa7fa73df84bb76.png","DAVID CROW" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/854afc254100ea95d00aa7a9a0f331f8.png","ROBERT WRIGHT" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/112b81246598c197c15883c07ecbbd9a.png","GORDON BROWN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/742b7a21f3a64b6e75916cdf784d9b0d.png","MATTHEW C KLEIN" to "https://thumbor.ftacademy.cn/unsafe/images/2022/07/d9ce64fefd33a42a46763e424e3dd898.png","DIANE COYLE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/08/5e4a34be7f0fd7735296b1debc2b0a13.png","MARK VANDEVELDE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/08/d8c71d8353646684dd9694013cf5988a.png","SIMON KUPER" to "https://thumbor.ftacademy.cn/unsafe/images/2022/08/9e6422d70a2f25e9208ca663a8aeef9c.png","AKILA QUINIO" to "https://thumbor.ftacademy.cn/unsafe/images/2022/08/08cd841fbeb753d372c7437c9ad4cc50.png","ALICE ROSS" to "https://thumbor.ftacademy.cn/unsafe/images/2022/08/51f9510746431366c74b3cab5c392353.png","MYLES MCCORMICK" to "https://thumbor.ftacademy.cn/unsafe/images/2022/08/d791674f4fc9b23668771d920d4a989d.png","CRISTINA CRIDDLE" to "https://thumbor.ftacademy.cn/unsafe/images/2022/08/0c172cb87a59cc5dd24f870d615ea929.png","JOHN REED" to "https://thumbor.ftacademy.cn/unsafe/images/2022/08/2034b5e57e2bafac6d6a8c2c391865e6.png")


class TemplateBuilder(private val template: String) {

    private val ctx: MutableMap<String, String> = HashMap()
    private var language: Language = Language.CHINESE
    private var shouldHideAd = false

    init {
        ctx["{{googletagservices-js}}"] = JSCodes.googletagservices
    }

    fun setLanguage(lang: Language): TemplateBuilder {
        this.language = lang
        return this
    }

    fun withFollows(follows: Map<String, String>): TemplateBuilder {

        Log.i(TAG, "$follows")
        follows.forEach { (key, value) ->
            ctx[key] = value
        }

        return this
    }

    fun withUserInfo(account: Account?): TemplateBuilder {
        if (account == null) {
            return this
        }
        ctx["<!-- AndroidUserInfo -->"] = """
<script>
var androidUserInfo = ${account.toJsonString()};
</script>""".trimIndent()

        return this
    }

    fun withAddress(addr: Address): TemplateBuilder {
        ctx["<!-- AndroidUserAddress -->"] = """
<script>
var androidUserAddress = ${addr.toJsonString()}
</script>""".trimIndent()

        return this
    }

    fun withStory(story: Story): TemplateBuilder {
        val (shouldHideAd, sponsorTitle) = story.shouldHideAd()

        var body = ""
        var title = ""
        var lang = ""
        when (this.language) {
            Language.CHINESE -> {
                body = story.getCnBody(withAd = !shouldHideAd)
                title = story.titleCN
                lang = "cn"
            }
            Language.ENGLISH -> {
                body = story.getEnBody(withAd = !shouldHideAd)
                title = story.titleEN
                lang = "en"
            }
            Language.BILINGUAL -> {
                body = story.getBilingualBody()
                title = "${story.titleCN}<br>${story.titleEN}"
                lang = "ce"
            }
        }

        // MARK: - Show a prefix for special events such as the 8.31 promotion. For now, only do this for tags because Ad sales might have different requirements.
        var storyPrefix = ""
        if (story.tag.contains(Regex("高端限免|17周年大视野精选|高端限免|17周年大視野精選"))) {
            storyPrefix = "【高端限免】"
        } else if (story.tag.contains(Regex("限免"))) {
            storyPrefix = "【限免】"
        }
        title = storyPrefix + title

        // MARK: - Show big headline for paid content
        var storyHeadlineClass = ""
        if (story.tag.contains(Regex("专享|限免|精选|双语阅读|專享|限免|精選|雙語閱讀")) || story.whitelist == 1) {
            storyHeadlineClass = " story-headline-large"
        }

        // MARK: - Get story headline styles and headshots
        val storyHeadlineStyle = getStoryHeaderStyle(story, body)
        val fullGridClass = if (body.contains("full-grid")) " full-grid-story" else ""
        val scrollyTellingClass = if (body.contains("scrollable-block")) " has-scrolly-telling" else ""
        val storyHeaderStyleClass = storyHeadlineStyle.style
        val storyBodyClass = fullGridClass + scrollyTellingClass + storyHeaderStyleClass
        val storyImageNoContainer = "<figure data-url=\"${story.cover.smallbutton}\" class=\"loading\"></figure>"
        ctx["<!--{story-body-class}-->"] = storyBodyClass
        ctx["<!--{story-image-no-container}-->"] = storyImageNoContainer
        ctx["<!--{Story-Header-Background}-->"] = storyHeadlineStyle.background
        ctx["<!--{story-author-headcut}-->"] = storyHeadlineStyle.headshot


        // todo
        ctx["{{story-css}}"] = ""
        ctx["{story-tag}"] = story.tag
        ctx["{story-author}"] = story.authorCN ?: ""
        ctx["{story-genre}"] = story.genre
        ctx["{story-area}"] = story.area
        ctx["{story-industry}"] = story.industry
        ctx["{story-main-topic}"] = ""
        ctx["{story-sub-topic}"] = ""
        ctx["{comments-id}"] = story.teaser?.getCommentsId() ?: ""
        // todo
        ctx["{{story-js-key}}"] = ""
        ctx["{{ad-pollyfill-js}}"] = ""
        ctx["{{db-zone-helper-js}}"] = ""



        val adTopic = story.getAdTopic()
        val cntopicScript = if (adTopic.isBlank()) "" else "window.cntopic = '$adTopic'"

        ctx["<!--{{cntopic}}-->"] = cntopicScript

        ctx["{story-language-class}"] = lang

        ctx["{Top-Banner}"] = """
            <div class="o-ads" data-o-ads-name="banner1"
            data-o-ads-center="true"
            data-o-ads-formats-default="false"
            data-o-ads-formats-small="FtcMobileBanner"
            data-o-ads-formats-medium="false"
            data-o-ads-formats-large="FtcLeaderboard"
            data-o-ads-formats-extra="FtcLeaderboard"
            data-o-ads-targeting="cnpos=top1;">
            </div>
        """.trimIndent()

        ctx["{Bottom-Banner}"] = """
            <div class="o-ads" data-o-ads-name="banner2"
            data-o-ads-center="true"
            data-o-ads-formats-default="false"
            data-o-ads-formats-small="FtcMobileBanner"
            data-o-ads-formats-medium="false"
            data-o-ads-formats-large="FtcBanner"
            data-o-ads-formats-extra="FtcBanner"
            data-o-ads-targeting="cnpos=top2;">
            </div>
        """.trimIndent()

        // Follow button
        ctx["{story-theme}"] = story.htmlForTheme(sponsorTitle)
        ctx["<!--{story-headline-class}-->"] = storyHeadlineClass
        // headline. Shown two times: one in title tag
        // other in body.
        ctx["{story-headline}"] = title
        // Lead-in
        ctx["{story-lead}"] = story.standfirstCN
        // Cover image
        ctx["{story-image}"] = story.htmlForCoverImage()
        ctx["{story-time}"] = story.formatPublishTime()
        ctx["{story-byline}"] = story.byline
        ctx["{story-body}"] = body

        ctx["{comments-order}"] = story.teaser?.getCommentsOrder() ?: ""

        // side-container
        ctx["{Right-1}"] = ""
        ctx["{story-container-style}"] = ""
        ctx["{related-stories}"] = story.htmlForRelatedStories()
        ctx["{related-topics}"] = story.htmlForRelatedTopics()

        // {ad-zone} Google广告输入正确的zone，输入值。
        // zone代表广告内容定向，发送给Google。
        // 数据来源：html div数组。

        ctx["{ad-zone}"] = story.getAdZone(Teaser.HOME_AD_ZONE, Teaser.DEFAULT_STORY_AD_ZONE, story.teaser?.channelMeta?.adZone ?: "")

        ctx["{ad-mpu}"] = if (shouldHideAd) "" else AdParser.getAdCode(AdPosition.MIDDLE_ONE)

        ctx["{adchID}"] = story.pickAdchID(Teaser.HOME_AD_CH_ID, Teaser.DEFAULT_STORY_AD_CH_ID)

        this.shouldHideAd = shouldHideAd

        return this
    }

    fun withTheme(isLight: Boolean): TemplateBuilder {
        ctx["{night-class}"] = if (isLight) "" else "night"
        return this
    }

    fun withJs(snippets: String): TemplateBuilder {
        ctx["<!--android-scripts-->"] = snippets
        return this
    }

    fun render(): String {

        var result = template

        ctx.forEach { (key, value) ->
            result = result.replace(key, value)
        }

        return JSCodes.getCleanHTML(
            JSCodes.getInlineVideo(
                AdParser.updateAdCode(result, this.shouldHideAd)
            )
        )
    }

    fun withChannel(content: String): TemplateBuilder {
        ctx["{list-content}"] = content
        return this
    }

    fun withSearch(keyword: String): TemplateBuilder {
        ctx["{search-html}"] = ""
        ctx["/*run-android-search-js*/"] = JsSnippets.search(keyword)
        return this
    }

    private data class StoryHeaderStyle(val style: String, val headshot: String, val background: String)
    private fun getStoryHeaderStyle(story: Story, body: String): StoryHeaderStyle {
        val tag = story.tag
        val imageHTML = story.htmlForCoverImage()
        val heroClass = " show-story-hero-container"
        val columnistClass = " show-story-columnist-topper"
        val pinkBackgroundClass = " story-hero-theme-pink"
        val englishAuthorName = story.authorEN.uppercase()
        if (imageHTML != "") {
            if (body.contains("full-grid") || body.contains("scrollable-block")) {
                return StoryHeaderStyle(heroClass, "", "")
            } else if (tag.contains(Regex("FT大视野|卧底经济学家|FT杂志|FT大視野|臥底經濟學家|FT雜誌"))) {
                return StoryHeaderStyle(heroClass, "", "")
            } else if (tag.contains(Regex("周末随笔|周末隨筆"))) {
                return StoryHeaderStyle(heroClass, "", pinkBackgroundClass)
            } else if (story.genre.contains(Regex("comment|opinion|column|feature")) && !tag.contains(Regex("FT商学院|FT商學院"))) {
                val columnPicUrl = headshots[englishAuthorName] ?: story.columnInfo?.headshot
                columnPicUrl?.let {
                    val headshot = "<figure data-url=\"${it}\" class=\"loading\"></figure>"
                    return StoryHeaderStyle(columnistClass, headshot, "")
                }
            }
        }
        return StoryHeaderStyle("", "", "")
    }
}
