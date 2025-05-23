package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsHtml
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.DurationUtils
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.value.BooleanValue
import net.sf.saxon.value.StringValue
import org.nineml.coffeefilter.InvisibleXml
import org.nineml.coffeefilter.InvisibleXmlFailureDocument
import org.nineml.coffeefilter.InvisibleXmlParser
import org.nineml.coffeefilter.ParserOptions
import org.nineml.coffeefilter.trees.SimpleTreeBuilder
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.ie.InternetExplorerOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.interactions.WheelInput
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.safari.SafariDriver
import org.openqa.selenium.safari.SafariOptions
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.xml.transform.sax.SAXSource
import kotlin.collections.iterator
import kotlin.math.min

class SeleniumStep(): AbstractAtomicStep() {
    companion object {
        val ns: NamespaceUri = NamespaceUri.of("https://xmlcalabash.com/ext/ns/selenium")
        val selenium = QName(ns, "x:selenium")

        val _all = QName(NamespaceUri.NULL, "all")
        val _arguments = QName(NamespaceUri.NULL, "arguments")
        val _browser = QName(NamespaceUri.NULL, "browser")
        val _call = QName(NamespaceUri.NULL, "call")
        val _capabilities = QName(NamespaceUri.NULL, "capabilities")
        val _char = QName(NamespaceUri.NULL, "char")
        val _click = QName(NamespaceUri.NULL, "click")
        val _close = QName(NamespaceUri.NULL, "close")
        val _cookie = QName(NamespaceUri.NULL, "cookie")
        val _deltaX = QName(NamespaceUri.NULL, "delta-x")
        val _deltaY = QName(NamespaceUri.NULL, "delta-y")
        val _direction = QName(NamespaceUri.NULL, "direction")
        val _drag = QName(NamespaceUri.NULL, "drag")
        val _find = QName(NamespaceUri.NULL, "find")
        val _if = QName(NamespaceUri.NULL, "if")
        val _key = QName(NamespaceUri.NULL, "key")
        val _message = QName(NamespaceUri.NULL, "message")
        val _move = QName(NamespaceUri.NULL, "move")
        val _navigate = QName(NamespaceUri.NULL, "navigate")
        val _output = QName(NamespaceUri.NULL, "output")
        val _page = QName(NamespaceUri.NULL, "page")
        val _param = QName(NamespaceUri.NULL, "param")
        val _pause = QName(NamespaceUri.NULL, "pause")
        val _perform = QName(NamespaceUri.NULL, "perform")
        val _property = QName(NamespaceUri.NULL, "property")
        val _refresh = QName(NamespaceUri.NULL, "refresh")
        val _release = QName(NamespaceUri.NULL, "release")
        val _send = QName(NamespaceUri.NULL, "send")
        val _set = QName(NamespaceUri.NULL, "set")
        val _script = QName(NamespaceUri.NULL, "script")
        val _scroll = QName(NamespaceUri.NULL, "scroll")
        val _string = QName(NamespaceUri.NULL, "string")
        val _subroutine = QName(NamespaceUri.NULL, "subroutine")
        val _type = QName(NamespaceUri.NULL, "type")
        val _until = QName(NamespaceUri.NULL, "until")
        val _wait = QName(NamespaceUri.NULL, "wait")
        val _while = QName(NamespaceUri.NULL, "while")
        val _whitelist = QName(NamespaceUri.NULL, "whitelist")
        val _xpath = QName(NamespaceUri.NULL, "xpath")
    }

    private val keymap = mapOf(
        "ADD" to Keys.ADD,
        "ALT" to Keys.ALT,
        "ARROW_DOWN" to Keys.ARROW_DOWN,
        "ARROW_LEFT" to Keys.ARROW_LEFT,
        "ARROW_RIGHT" to Keys.ARROW_RIGHT,
        "ARROW_UP" to Keys.ARROW_UP,
        "BACK_SPACE" to Keys.BACK_SPACE,
        "CANCEL" to Keys.CANCEL,
        "CLEAR" to Keys.CLEAR,
        "COMMAND" to Keys.COMMAND,
        "CONTROL" to Keys.CONTROL,
        "DECIMAL" to Keys.DECIMAL,
        "DELETE" to Keys.DELETE,
        "DIVIDE" to Keys.DIVIDE,
        "DOWN" to Keys.DOWN,
        "END" to Keys.END,
        "ENTER" to Keys.ENTER,
        "EQUALS" to Keys.EQUALS,
        "ESCAPE" to Keys.ESCAPE,
        "F1" to Keys.F1,
        "F10" to Keys.F10,
        "F11" to Keys.F11,
        "F12" to Keys.F12,
        "F2" to Keys.F2,
        "F3" to Keys.F3,
        "F4" to Keys.F4,
        "F5" to Keys.F5,
        "F6" to Keys.F6,
        "F7" to Keys.F7,
        "F8" to Keys.F8,
        "F9" to Keys.F9,
        "HELP" to Keys.HELP,
        "HOME" to Keys.HOME,
        "INSERT" to Keys.INSERT,
        "LEFT" to Keys.LEFT,
        "LEFT_ALT" to Keys.LEFT_ALT,
        "LEFT_CONTROL" to Keys.LEFT_CONTROL,
        "LEFT_SHIFT" to Keys.LEFT_SHIFT,
        "META" to Keys.META,
        "MULTIPLY" to Keys.MULTIPLY,
        "NULL" to Keys.NULL,
        "NUMPAD0" to Keys.NUMPAD0,
        "NUMPAD1" to Keys.NUMPAD1,
        "NUMPAD2" to Keys.NUMPAD2,
        "NUMPAD3" to Keys.NUMPAD3,
        "NUMPAD4" to Keys.NUMPAD4,
        "NUMPAD5" to Keys.NUMPAD5,
        "NUMPAD6" to Keys.NUMPAD6,
        "NUMPAD7" to Keys.NUMPAD7,
        "NUMPAD8" to Keys.NUMPAD8,
        "NUMPAD9" to Keys.NUMPAD9,
        "PAGE_DOWN" to Keys.PAGE_DOWN,
        "PAGE_UP" to Keys.PAGE_UP,
        "PAUSE" to Keys.PAUSE,
        "RETURN" to Keys.RETURN,
        "RIGHT" to Keys.RIGHT,
        "SEPARATOR" to Keys.SEPARATOR,
        "SHIFT" to Keys.SHIFT,
        "SPACE" to Keys.SPACE,
        "SUBTRACT" to Keys.SUBTRACT,
        "TAB" to Keys.TAB,
        "UP" to Keys.UP
    )

    private var parser: InvisibleXmlParser? = null
    private lateinit var driver: RemoteWebDriver
    private lateinit var page: URI
    private lateinit var capabilities: Map<QName, XdmValue>
    private val arguments = mutableListOf<String>()
    private val findResults = mutableMapOf<String, FindResult>()
    private val subroutines = mutableMapOf<String, XdmNode>()
    private val whitelist = mutableListOf<Regex>()

    override fun run() {
        super.run()

        val config = stepConfig.xmlCalabashConfig.other[selenium] ?: emptyList()
        for (wlist in config) {
            val list = wlist[_whitelist] ?: ""
            for (regex in list.split("\\s+".toRegex())) {
                stepConfig.debug { "Selenium whitelist: ${regex}" }
                whitelist.add(Regex(regex))
            }
        }

        if (parser == null) {
            init()
        }

        val source = queues["source"]!!.first()
        val script = if (source.contentClassification == MediaClassification.TEXT) {
            val text = (source.value as XdmNode).underlyingValue.stringValue
            S9Api.documentElement(parseSource(text))
        } else {
            // TODO: validate the XML!
            S9Api.documentElement(source.value as XdmNode)
        }

        loadSubroutines(script)

        capabilities = qnameMapBinding(_capabilities)
        val args = options[_arguments]?.value ?: XdmEmptySequence.getInstance()
        val iter = args.iterator()
        while (iter.hasNext()) {
            arguments.add(iter.next().underlyingValue.stringValue)
        }

        val version = script.getAttributeValue(Ns.version)
        if (version != "0.2") {
            throw stepConfig.exception(XProcError.xdStepFailed("Invalid script version: ${version}"))
        }
        page = URI(script.getAttributeValue(_page))
        checkWhitelist(page.toString())

        val reqBrowser = stringBinding(_browser)
        val browser = if (reqBrowser != null) {
            reqBrowser
        } else {
            val propBrowser = System.getProperty("com.xmlcalabash.selenium.browser")
            propBrowser ?: "firefox" // Have to pick something!
        }

        driver = when (browser) {
            "firefox" -> setupFirefoxDriver()
            "chrome" -> setupChromeDriver()
            "safari" -> setupSafariDriver()
            "edge" -> setupEdgeDriver()
            "internetexplorer" -> setupIEDriver()
            else -> throw stepConfig.exception(XProcError.xdStepFailed("Invalid browser: ${browser}"))
        }

        try {
            driver.get("${page}")
            interpretScript(script)
        } finally {
            driver.quit()
            findResults.clear()
        }
    }

    private fun interpretScript(script: XdmNode) {
        val elements = mutableListOf<XdmNode>()
        for (node in script.axisIterator(Axis.CHILD)) {
            if (node.nodeKind == XdmNodeKind.ELEMENT) {
                elements.add(node)
            }
        }

        for (statement in elements) {
            when (statement.nodeName) {
                _output -> interpretOutput(statement)
                _find -> interpretFind(statement)
                _set -> interpretSet(statement)
                _cookie -> interpretCookie(statement)
                _send -> interpretSingle(statement)
                _click -> interpretSingle(statement)
                _scroll -> interpretSingle(statement)
                _drag -> interpretSingle(statement)
                _key -> interpretSingle(statement)
                _move -> interpretSingle(statement)
                _release -> interpretSingle(statement)
                _pause -> interpretSingle(statement)
                _perform -> interpretPerform(statement)
                _wait -> interpretWait(statement)
                _refresh -> interpretRefresh(statement)
                _message -> interpretMessage(statement)
                _navigate -> interpretNavigate(statement)
                _while -> interpretWhile(statement)
                _until -> interpretUntil(statement)
                _if -> interpretIf(statement)
                _call -> interpretCall(statement)
                _close -> return
                _subroutine -> Unit // already loaded
                else -> throw stepConfig.exception(XProcError.xdStepFailed("Unexpected statement: ${statement.nodeName}"))
            }
        }
    }

    private fun interpretPerform(statement: XdmNode) {
        val actions = Actions(driver)
        for (child in statement.axisIterator(Axis.CHILD)) {
            if (child.nodeKind == XdmNodeKind.ELEMENT) {
                interpretAction(child, actions)
            }
        }
        actions.build().perform()
    }

    private fun interpretSingle(statement: XdmNode) {
        val actions = Actions(driver)
        interpretAction(statement, actions)
        actions.build().perform()
    }

    private fun interpretAction(statement: XdmNode, actions: Actions) {
        when (statement.nodeName) {
            _send -> interpretSend(statement, actions)
            _key -> interpretKey(statement, actions)
            _click -> interpretClick(statement, actions)
            _scroll -> interpretScroll(statement, actions)
            _move -> interpretMove(statement, actions)
            _release -> interpretRelease(statement, actions)
            _drag -> interpretDrag(statement, actions)
            _pause -> interpretPause(statement, actions)
            else -> throw stepConfig.exception(XProcError.xdStepFailed("Unexpected statement: ${statement.nodeName}"))
        }
    }

    private fun interpretFind(element: XdmNode) {
        val all = element.getAttributeValue(_all)
        if (all == null) {
            interpretFindElement(element)
        } else {
            interpretFindAllElements(element)
        }
    }

    private fun interpretFindElement(element: XdmNode) {
        val name = element.getAttributeValue(Ns.name)!!
        val find = element.getAttributeValue(_string)!!
        val type = element.getAttributeValue(_type)!!
        val waitAttr = element.getAttributeValue(_wait)
        val pauseAttr = element.getAttributeValue(_pause)

        val wait = if (waitAttr != null || pauseAttr != null) {
            DurationUtils.parseDuration(stepConfig, waitAttr ?: "PT30S")
        } else {
            null
        }

        val pause = if (waitAttr != null || pauseAttr != null) {
            DurationUtils.parseDuration(stepConfig, pauseAttr ?: "PT0.25S")
        } else {
            null
        }

        val start = Instant.now()

        while (true) {
            try {
                val element = when (type) {
                    "name" -> driver.findElement(By.name(find))
                    "selector" -> driver.findElement(By.cssSelector(find))
                    "id" -> driver.findElement(By.id(find))
                    "link-text" -> driver.findElement(By.linkText(find))
                    "partial-link-text" -> driver.findElement(By.partialLinkText(find))
                    "tag" -> driver.findElement(By.tagName(find))
                    "class" -> driver.findElement(By.className(find))
                    "xpath" -> driver.findElement(By.xpath(find))
                    else -> {
                        throw stepConfig.exception(XProcError.xdStepFailed("Unexpected find type: ${type}"))
                    }
                }
                val domSerialization = element.getDomProperty("outerHTML")!!
                val node = nodeFor(domSerialization)
                findResults[name] = FindResult(node, element)
                return
            } catch (_: NoSuchElementException) {
                if (wait != null) {
                    stepConfig.debug { "Did not find \$${name}; pausing ${pause} for up to ${wait}."}
                } else {
                    stepConfig.debug { "Did not find \$${name}"}
                }
                val interval = Duration.between(start, Instant.now())
                if (wait == null || interval > wait) {
                    findResults[name] = FindResult()
                    return
                }
                Thread.sleep(pause!!.toMillis())
            }
        }
    }

    private fun interpretFindAllElements(element: XdmNode) {
        val name = element.getAttributeValue(Ns.name)!!
        val find = element.getAttributeValue(_string)!!
        val type = element.getAttributeValue(_type)!!

        try {
            val elements = when (type) {
                "name" -> driver.findElements(By.name(find))
                "selector" -> driver.findElements(By.cssSelector(find))
                "id" -> driver.findElements(By.id(find))
                "link-text" -> driver.findElements(By.linkText(find))
                "partial-link-text" -> driver.findElements(By.partialLinkText(find))
                "tag" -> driver.findElements(By.tagName(find))
                "class" -> driver.findElements(By.className(find))
                "xpath" -> driver.findElements(By.xpath(find))
                else -> {
                    throw stepConfig.exception(XProcError.xdStepFailed("Unexpected find type: ${type}"))
                }
            }
            val nodes = mutableListOf<XdmValue>()
            for (element in elements) {
                nodes.add(nodeFor(element.getDomProperty("outerHTML")!!))
            }
            findResults[name] = FindResult(nodes, elements)
        } catch (_: NoSuchElementException) {
            findResults[name] = FindResult()
        }
    }

    private fun interpretSet(element: XdmNode) {
        val name = element.getAttributeValue(Ns.name)!!
        val type = element.getAttributeValue(Ns.type)!!

        val value = when (type) {
            "window" -> {
                val param = element.getAttributeValue(_param)!!
                val window = driver.manage().window()
                when (param) {
                    "width" -> "${window.size.width}"
                    "height" -> "${window.size.height}"
                    "x" -> "${window.position.x}"
                    "y" -> "${window.position.y}"
                    else -> throw stepConfig.exception(XProcError.xdStepFailed("Unexpected window param type: ${param}"))
                }
            }
            "page" -> {
                val param = element.getAttributeValue(_param)!!
                when (param) {
                    "title" -> "${driver.title}"
                    "url" -> "${driver.currentUrl}"
                    else -> throw stepConfig.exception(XProcError.xdStepFailed("Unexpected page param type: ${param}"))
                }
            }
            "string" -> {
                element.getAttributeValue(_string)!!
            }
            "xpath" -> {
                val expr = element.getAttributeValue(_xpath)!!
                val selector = selector(expr)
                selector.evaluate().toString()
            }
            "element" -> {
                val from = element.getAttributeValue(Ns.from)!!
                val prop = element.getAttributeValue(_property)
                val result = usefulResult(from)
                if (prop != "value") {
                    throw stepConfig.exception(XProcError.xdStepFailed("Unexpected set element property: ${prop}"))
                }
                result.elements[0].getDomProperty("value")
            }
            "cookie" -> {
                val cookieName = element.getAttributeValue(_cookie)!!
                val cookie = driver.manage().getCookieNamed(cookieName)
                cookie?.value ?: ""
            }
            else -> throw stepConfig.exception(XProcError.xdStepFailed("Unexpected set type: ${type}"))
        }

        findResults[name] = FindResult(XdmAtomicValue(value))
    }

    private fun interpretCookie(element: XdmNode) {
        val name = element.getAttributeValue(Ns.name)!!
        val value = element.getAttributeValue(Ns.value)!!
        val path = element.getAttributeValue(Ns.path) ?: "/"
        val duration = element.getAttributeValue(Ns.duration)

        var builder = Cookie.Builder(name, value)
            .path(path)

        if (duration != null) {
            val instant = Instant.now().plus(DurationUtils.parseDuration(stepConfig, duration))
            builder = builder.expiresOn(Date.from(instant))
        }

        val cookie = builder.build()
        driver.manage().addCookie(cookie)
    }

    private fun interpretSend(element: XdmNode, actions: Actions) {
        checkWhitelist(driver.currentUrl)

        val _string = QName(NamespaceUri.NULL, "string")

        val name = element.getAttributeValue(Ns.name)
        var text = element.getAttributeValue(_string)
        if (text == null) {
            val baos = ByteArrayOutputStream()
            val writer = DocumentWriter(XProcDocument.ofXml(element, stepConfig), baos)
            writer[Ns.method] = "text"
            writer[Ns.encoding] = "utf-8"
            writer.write()
            text = baos.toString(StandardCharsets.UTF_8)
        }

        if (name != null) {
            val result = usefulResult(name)
            for (element in result.elements) {
                actions.sendKeys(element, text)
            }
        } else {
            actions.sendKeys(text)
        }
    }

    private fun interpretKey(key: XdmNode, actions: Actions) {
        checkWhitelist(driver.currentUrl)

        val direction = key.getAttributeValue(_direction)!!
        val name = key.getAttributeValue(Ns.name)

        if (name != null) {
            val skey = keymap[name]!!
            if (direction == "up") {
                actions.keyUp(skey)
            } else {
                actions.keyDown(skey)
            }
            return
        }

        val char = key.getAttributeValue(_char)!!
        var index = 0
        while (index < char.length) {
            val cp = char.codePointAt(index)
            index += Character.charCount(cp)
            val seq = String(Character.toChars(cp))
            if (direction == "up") {
                actions.keyUp(seq)
            } else {
                actions.keyDown(seq)
            }
        }
    }

    private fun interpretClick(click: XdmNode, actions: Actions) {
        checkWhitelist(driver.currentUrl)

        val name = click.getAttributeValue(Ns.name)!!
        val type = click.getAttributeValue(Ns.type)!!
        val result = usefulResult(name)
        for (element in result.elements) {
            when (type) {
                "click" -> actions.click(element)
                "doubleclick" -> actions.doubleClick(element)
                "click-hold" -> actions.clickAndHold(element)
                else -> throw stepConfig.exception(XProcError.xdStepFailed("Unexpected click type: ${type}"))
            }
        }
    }

    private fun interpretScroll(scroll: XdmNode, actions: Actions) {
        val from = scroll.getAttributeValue(Ns.from)
        val to = scroll.getAttributeValue(Ns.to)
        val dx = scroll.getAttributeValue(_deltaX)?.toInt() ?: 0
        val dy = scroll.getAttributeValue(_deltaY)?.toInt() ?: 0

        if (to != null) {
            val result = usefulResult(to)
            // In Firefox, moveToElement won't move outside the current viewport.
            // Hackaroonie!
            val js = driver as JavascriptExecutor
            for (element in result.elements) {
                js.executeScript("arguments[0].scrollIntoView(true);", element)
            }
            return
        }

        if (from != null) {
            val result = usefulResult(from)
            for (element in result.elements) {
                val origin = WheelInput.ScrollOrigin.fromElement(element)
                actions.scrollFromOrigin(origin, dx, dy)
            }
            return
        }

        actions.scrollByAmount(dx, dy)
    }

    private fun interpretDrag(drag: XdmNode, actions: Actions) {
        val from = drag.getAttributeValue(Ns.from)!!
        val to = drag.getAttributeValue(Ns.to)!!

        val fromResult = usefulResult(from)
        val toResult = usefulResult(to)

        actions.dragAndDrop(fromResult.elements.first(), toResult.elements.first())
    }

    private fun interpretMove(scroll: XdmNode, actions: Actions) {
        val to = scroll.getAttributeValue(Ns.to)!!

        val result = usefulResult(to)
        for (element in result.elements) {
            actions.moveToElement(element)
            if (element.tagName.lowercase() == "input") {
                actions.click()
            }
        }
    }

    private fun interpretRelease(@Suppress("UNUSED_PARAMETER") release: XdmNode, actions: Actions) {
        actions.release()
    }

    private fun interpretPause(pause: XdmNode, actions: Actions) {
        val time = pause.getAttributeValue(Ns.duration)!!
        val duration = DurationUtils.parseDuration(stepConfig, time)
        actions.pause(duration)
    }

    private fun interpretWait(@Suppress("UNUSED_PARAMETER") wait: XdmNode) {
        var state = driver.executeScript("return document.readyState;")?.toString()
        while (state != "complete") {
            Thread.sleep(100)
            state = driver.executeScript("return document.readyState;")?.toString()
        }
    }

    private fun interpretRefresh(@Suppress("UNUSED_PARAMETER") refresh: XdmNode) {
        driver.navigate().refresh()
    }

    private fun interpretOutput(output: XdmNode) {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(page)

        val name = output.getAttributeValue(Ns.name)
        val string = output.getAttributeValue(_string)

        if (string != null) {
            val type = output.getAttributeValue(Ns.type)
            if (type == "xpath") {
                val selector = selector(string)
                builder.addText(selector.evaluate().toString())
            } else {
                builder.addText(string)
            }
        } else {
            val nodes = mutableListOf<XdmValue>()
            if (name == null) {
                nodes.add(nodeFor(driver.pageSource!!))
            } else {
                val result = findResults[name]
                    ?: throw stepConfig.exception(XProcError.xdStepFailed("No '${name}' element selected"))
                nodes.addAll(result.values)
            }
            for (node in nodes) {
                builder.addSubtree(node)
            }
        }

        builder.endDocument()
        val result = builder.result

        if (S9Api.isTextDocument(result)) {
            receiver.output("result", XProcDocument.ofText(result, stepConfig))
        } else {
            receiver.output("result", XProcDocument.ofXml(result, stepConfig))
        }
    }

    private fun interpretMessage(message: XdmNode) {
        val select = message.getAttributeValue(Ns.select)!!
        val selector = selector(select)
        stepConfig.info { selector.evaluate().toString() }
    }

    private fun interpretNavigate(navigate: XdmNode) {
        val to = navigate.getAttributeValue(Ns.to)
        if (to != null) {
            val url = UriUtils.resolve(URI(driver.currentUrl!!),to)!!.toURL()
            checkWhitelist(to)
            driver.navigate().to(url)
            return
        }

        val direction = navigate.getAttributeValue(_direction)!!
        if (direction.startsWith("for")) {
            driver.navigate().forward()
        } else {
            driver.navigate().back()
        }
    }

    private fun interpretIf(element: XdmNode) {
        if (checkTest(element)) {
            interpretScript(element)
        }
    }

    private fun interpretWhile(element: XdmNode) {
        var loop = checkTest(element)
        while (loop) {
            interpretScript(element)
            loop = checkTest(element)
        }
    }

    private fun interpretUntil(element: XdmNode) {
        interpretScript(element)
        var done = checkTest(element)
        while (!done) {
            interpretScript(element)
            done = checkTest(element)
        }
    }

    private fun checkTest(element: XdmNode): Boolean {
        val test = element.getAttributeValue(Ns.test)!!
        val selector = selector(test)
        return selector.effectiveBooleanValue()
    }

    private fun selector(expression: String): XPathSelector {
        val compiler = stepConfig.newXPathCompiler()
        for ((name, _) in findResults) {
            val qname = QName(NamespaceUri.NULL, name)
            compiler.declareVariable(qname)
        }
        val exec = compiler.compile(expression)
        val selector = exec.load()
        for ((name, result) in findResults) {
            val qname = QName(NamespaceUri.NULL, name)
            var variableValue: XdmValue = XdmEmptySequence.getInstance()
            for (value in result.values) {
                variableValue = variableValue.append(value)
            }
            selector.setVariable(qname, variableValue)
        }
        return selector
    }

    private fun interpretCall(element: XdmNode) {
        val name = element.getAttributeValue(Ns.name)!!
        val body = subroutines[name]
        if (body == null) {
            throw stepConfig.exception(XProcError.xdStepFailed("No function named '${name}' exists"))
        }
        interpretScript(body)
    }

    private fun setupChromeDriver(): ChromeDriver {
        val options = ChromeOptions()
        for (arg in arguments) {
            options.addArguments(arg)
        }
        for ((key, value) in capabilities) {
            if (key.namespaceUri == NamespaceUri.NULL) {
                val name = key.localName
                when (value.underlyingValue) {
                    is StringValue -> options.setCapability(name, value.underlyingValue.stringValue)
                    is BooleanValue -> options.setCapability(name, value.underlyingValue.effectiveBooleanValue())
                    else -> throw stepConfig.exception(XProcError.xdStepFailed("Unknown value type: ${value.underlyingValue}"))
                }
            }
        }
        val driver = ChromeDriver(options)
        return driver
    }

    private fun setupFirefoxDriver(): FirefoxDriver {
        val options = FirefoxOptions()
        for (arg in arguments) {
            options.addArguments(arg)
        }
        for ((key, value) in capabilities) {
            if (key.namespaceUri == NamespaceUri.NULL) {
                val name = key.localName
                when (value.underlyingValue) {
                    is StringValue -> options.setCapability(name, value.underlyingValue.stringValue)
                    is BooleanValue -> options.setCapability(name, value.underlyingValue.effectiveBooleanValue())
                    else -> throw stepConfig.exception(XProcError.xdStepFailed("Unknown value type: ${value.underlyingValue}"))
                }
            }
        }
        val driver = FirefoxDriver(options)
        return driver
    }

    private fun setupSafariDriver(): SafariDriver {
        val options = SafariOptions()
        if (arguments.isNotEmpty()) {
            stepConfig.warn { "Arguments are ignored on the Safari web driver" }
        }
        for ((key, value) in capabilities) {
            if (key.namespaceUri == NamespaceUri.NULL) {
                val name = key.localName
                when (value.underlyingValue) {
                    is StringValue -> options.setCapability(name, value.underlyingValue.stringValue)
                    is BooleanValue -> options.setCapability(name, value.underlyingValue.effectiveBooleanValue())
                    else -> throw stepConfig.exception(XProcError.xdStepFailed("Unknown value type: ${value.underlyingValue}"))
                }
            }
        }
        val driver = SafariDriver(options)
        return driver
    }

    private fun setupEdgeDriver(): EdgeDriver {
        val options = EdgeOptions()
        for (arg in arguments) {
            options.addArguments(arg)
        }
        for ((key, value) in capabilities) {
            if (key.namespaceUri == NamespaceUri.NULL) {
                val name = key.localName
                when (value.underlyingValue) {
                    is StringValue -> options.setCapability(name, value.underlyingValue.stringValue)
                    is BooleanValue -> options.setCapability(name, value.underlyingValue.effectiveBooleanValue())
                    else -> throw stepConfig.exception(XProcError.xdStepFailed("Unknown value type: ${value.underlyingValue}"))
                }
            }
        }
        val driver = EdgeDriver(options)
        return driver
    }

    private fun setupIEDriver(): InternetExplorerDriver {
        val options = InternetExplorerOptions()
        for (arg in arguments) {
            options.addCommandSwitches(arg)
        }
        for ((key, value) in capabilities) {
            if (key.namespaceUri == NamespaceUri.NULL) {
                val name = key.localName
                when (value.underlyingValue) {
                    is StringValue -> options.setCapability(name, value.underlyingValue.stringValue)
                    is BooleanValue -> options.setCapability(name, value.underlyingValue.effectiveBooleanValue())
                    else -> throw stepConfig.exception(XProcError.xdStepFailed("Unknown value type: ${value.underlyingValue}"))
                }
            }
        }
        val driver = InternetExplorerDriver(options)
        return driver
    }

    private fun init() {
        val invisibleXml = InvisibleXml()
        val stream = SeleniumStep::class.java.getResourceAsStream("/com/xmlcalabash/ext/selenium-grammar.ixml")
        parser = invisibleXml.getParser(stream, "https://xmlcalabash.com/ext/selenium/selenium-grammar.ixml")
    }

    private fun nodeFor(markup: String): XdmNode {
        val endpos = if (markup.indexOf(" ") > 0) {
            min(markup.indexOf(">"), markup.indexOf(" "))
        } else {
            markup.indexOf(">")
        }
        val tagname = QName(NsHtml.namespace, markup.substring(1, endpos).lowercase())

        val wholeDocument = tagname == NsHtml.html

        // If we're only going to parse a bit of the page, make sure we set up the
        // input so that the parser will work out the correct encoding.
        val stream = if (!wholeDocument) {
            // Try to special case tables...
            val sb = StringBuilder()
            sb.append("<html><head><meta charset='UTF-8'>")

            when (tagname.localName) {
                "thead", "tbody", "tfoot" -> sb.append("<table>")
                "tr" -> sb.append("<table><tbody>")
                "th", "td" -> sb.append("<table><tbody><tr>")
                else -> Unit
            }

            sb.append(markup)

            ByteArrayInputStream(sb.toString().toByteArray(StandardCharsets.UTF_8))
        } else {
            ByteArrayInputStream(markup.toByteArray(StandardCharsets.UTF_8))
        }

        val loader = DocumentLoader(stepConfig, page)
        val doc = loader.load(stream, MediaType.HTML).value as XdmNode
        if (wholeDocument) {
            return doc
        }

        // If the markup doesn't contain <html, assume that we're looking at a
        // part of the page. In this case, we want only the content of the body.
        // This is an absolute hack.
        val node = findNode(S9Api.documentElement(doc), tagname)
        return node ?: doc
    }

    private fun findNode(node: XdmNode, name: QName): XdmNode? {
        for (child in node.axisIterator(Axis.CHILD)) {
            if (child.nodeKind != XdmNodeKind.ELEMENT) {
                continue
            }

            if (child.nodeName == name) {
                return child
            } else {
                val descendant = findNode(child, name)
                if (descendant != null) {
                    return descendant
                }
            }
        }

        return null
    }

    private fun parseSource(text: String): XdmNode {
        val doc = parser!!.parse(text)

        val opts = ParserOptions()
        opts.assertValidXmlNames = false
        opts.assertValidXmlCharacters = false
        val tree = SimpleTreeBuilder(opts)

        if (!doc.succeeded()) {
            val failure = doc as InvisibleXmlFailureDocument
            failure.getTree(tree)
            throw stepConfig.exception(XProcError.xcxSeleniumInvalidScript(tree.tree.asXML().toString()))
        } else {
            val walker = doc.result.arborist
            walker.getTree(doc.getAdapter(tree))
        }

        val builder = stepConfig.processor.newDocumentBuilder()
        builder.isLineNumbering = true
        val bytes = ByteArrayInputStream(tree.tree.asXML().toByteArray(StandardCharsets.UTF_8))
        val source = SAXSource(InputSource(bytes))
        val xdmDestination = XdmDestination()
        builder.parse(source, xdmDestination)

        return xdmDestination.xdmNode
    }

    private fun loadSubroutines(script: XdmNode) {
        val compiler = stepConfig.newXPathCompiler()
        val exec = compiler.compile("//subroutine")
        val selector = exec.load()
        selector.contextItem = script
        val iter = selector.evaluate().iterator()
        while (iter.hasNext()) {
            val subroutine = iter.next() as XdmNode
            if (subroutine.parent!!.nodeName != _script) {
                throw stepConfig.exception(XProcError.xdStepFailed("Subroutines are only allowed at the top level"))
            }
            val name = subroutine.getAttributeValue(Ns.name)!!
            if (subroutines.containsKey(name)) {
                throw stepConfig.exception(XProcError.xdStepFailed("Subroutines cannot be redefined: ${name}"))
            }
            subroutines[name] = subroutine
        }
    }

    private fun usefulResult(name: String): FindResult {
        val result = findResults[name]
            ?: throw stepConfig.exception(XProcError.xdStepFailed("No '${name}' element selected"))
        if (result.count == 0) {
            throw stepConfig.exception(XProcError.xdStepFailed("No '${name}' element was found"))
        }
        return result
    }

    private fun checkWhitelist(uri: String?) {
        if (uri == null || whitelist.isEmpty()) {
            return
        }

        for (regex in whitelist) {
            if (regex.matches(uri)) {
                stepConfig.debug { "Selenium whitelisted: ${uri}" }
                return
            }
            stepConfig.debug { "Selenium blacklisted: ${uri}" }
        }

        throw stepConfig.exception(XProcError.xcxSeleniumNotWhitelisted(uri))
    }

    override fun toString(): String = "cx:selenium"

    inner class FindResult(val values: List<XdmValue>, val elements: List<WebElement>) {
        val count: Int
            get() = elements.size
        constructor(value: XdmNode, element: WebElement): this(listOf(value), listOf(element))
        constructor(value: XdmValue): this(listOf(value), emptyList<WebElement>())
        constructor(): this(listOf(XdmEmptySequence.getInstance()), emptyList<WebElement>())
    }
}
