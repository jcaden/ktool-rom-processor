package com.kurento.ktool.rom.processor.codegen.function;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * 
 * Replace sphinx/restructured text roles by javadoc equivalents in Kurento
 * model.json documentation
 * 
 * @author Santiago Gala (sgala@apache.org)
 * 
 */
public class SphinxLinks implements TemplateMethodModelEx {

	/**
	 * 
	 * Takes a string and replaces occurrences of rst/sphinx with kurento domain
	 * markup with javadoc equivalents.
	 * 
	 * @param arguments
	 *            A list of arguments from the call. It only processes the first
	 *            one as a String
	 * 
	 * @see freemarker.template.TemplateMethodModelEx#exec(java.util.List)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments)
			throws TemplateModelException {

		Pattern glossary_term_1 = Pattern.compile(":term:`([^`<]*?)`");
		Pattern glossary_term_2 = Pattern
				.compile(":term:`([^`<]*?)<([^`]*?)>`");
		String glossary_href = "<a href=\"http://www.kurento.org/docs/current/glossary.html#term-%s\">%s</a>";
		// TODO: `<text>`, ** and *, other markup...
		String[][] toReplace = {
				{ ":wikipedia:`(.*?),(.*?)`", // Kurento wikipedia, alt
						"<a href=\"http://$1.wikipedia.org/wiki/$2\">$2</a>" },
				{ ":wikipedia:`(.*?)<(.*?),(.*?)>`", // Kurento wikipedia
						"<a href=\"http://$2.wikipedia.org/wiki/$3\">$1</a>" },
				{ ":java:ref:`([^`]*?)<(.*?)>`", // java ref, alternate title
						"{@link $2 $1}" }, { ":java:ref:`(.*?)`", // java ref
						"{@link $1}" },
				{ ":rom:cls:`([^`]*?)<([^`<]*?)>`", "{@link $2 $1}" },
				{ ":rom:cls:`([^`]*?)`", "{@link $1}" },
				{ ":rom:meth:`([^`]*?)<([^`]*?)>`", "{@link #$2 $1}" },
				{ ":rom:meth:`([^`]*?)`", "{@link #$1}" },
				{ ":rom:attr:`([^`]*?)<([^`]*?)>`", "{@link #$2 $1}" },
				{ ":rom:attr:`([^`]*?)`", "{@link #$1}" },
				{ ":rom:evt:`([^`]*?)<([^`]*?)>`", "{@link $2 $1Event}" },
				{ ":rom:evt:`([^`]*?)`", "{@link $1Event}" },
				{ ":author:", "@author" }, // author
				{ ":since:", "@since" }, // since
				{ "``([^`]*?)``", "<code>$1</code>" },
				{ "\\.\\.\\s+todo::(.*?)", "<hr/><b>TODO</b>$1" },
				{ "\\.\\.\\s+note::(.*?)", "<hr/><b>Note</b>$1" },
		};

		String typeName = arguments.get(0).toString();
		String res = typeName;

		res = translate(res, toReplace);
		Matcher m2 = glossary_term_2.matcher(res);
		while (m2.find()) {
			res = res.substring(0, m2.start() - 1)
					+ String.format(glossary_href, make_id(m2.group(2)),
							m2.group(1)) + res.substring(m2.end() + 1);
		}
		m2 = glossary_term_1.matcher(res);
		while (m2.find()) {
			res = res.substring(0, m2.start())
					+ String.format(glossary_href, make_id(m2.group(1)),
							m2.group(1)) + res.substring(m2.end());
		}

		return res;
	}

	/**
	 * Clone the python unicode translate method in legacy languages younger
	 * than python. python docutils is public domain.
	 * 
	 * @param text
	 *            string for which translation is needed
	 * @param patterns
	 *            Array of arrays {target, replacement). The target is
	 *            substituted by the replacement.
	 * @return The translated string
	 * @see http://docs.python.org/3/library/stdtypes.html#str.translate
	 */
	public String translate(String text, String[][] patterns) {
		String res = text;
		for (String[] each : patterns) {
			res = res.replaceAll("(?ms)" + each[0], each[1]);
		}
		return res;
	}

	/**
	 * Our use case is
	 * {@code $ python -c "from docutils import nodes; print('term-'+nodes.make_id('QR'))" }
	 * , which returns {@code term-qr } i.e., identifiers conforming to the
	 * regular expression [a-z](-?[a-z0-9]+)*
	 * 
	 * But there is a requirement to use <em>pure</em> java for this task. So we
	 * clone the function here. python docutils is public domain.
	 * 
	 * @see http
	 *      ://code.nabla.net/doc/docutils/api/docutils/nodes/docutils.nodes.
	 *      make_id.html
	 */
	public String make_id(String txt) {
		// id = string.lower()
		String id = txt.toLowerCase();
		// if not isinstance(id, unicode):
		// id = id.decode()
		// id = id.translate(_non_id_translate_digraphs)
		id = translate(id, _non_id_translate_digraphs);
		// id = id.translate(_non_id_translate)
		id = translate(id, _non_id_translate);
		// # get rid of non-ascii characters.
		// # 'ascii' lowercase to prevent problems with turkish locale.
		// id = unicodedata.normalize('NFKD', id).\
		// encode('ascii', 'ignore').decode('ascii')
		// # shrink runs of whitespace and replace by hyphen
		// id = _non_id_chars.sub('-', ' '.join(id.split()))
		id = id.replaceAll("\\s+", " ").replaceAll(_non_id_chars,
				"-");
		// id = _non_id_at_ends.sub('', id)
		id = id.replaceAll(_non_id_at_ends, "");
		// return str(id)
		return id;

	}

	//_non_id_chars = re.compile('[^a-z0-9]+')
	String _non_id_chars = "[^a-z0-9]+";
	//_non_id_at_ends = re.compile('^[-0-9]+|-+$')
	String _non_id_at_ends = "^[-0-9]+|-+$";
	String[][] _non_id_translate ={
			{"\u00f8", "o"}, // o with stroke
			{"\u0111", "d"}, // d with stroke
			{"\u0127", "h"}, // h with stroke
			{"\u0131", "i"}, // dotless i
			{"\u0142", "l"}, // l with stroke
			{"\u0167", "t"}, // t with stroke
			{"\u0180", "b"}, // b with stroke
			{"\u0183", "b"}, // b with topbar
			{"\u0188", "c"}, // c with hook
			{"\u018c", "d"}, // d with topbar
			{"\u0192", "f"}, // f with hook
			{"\u0199", "k"}, // k with hook
			{"\u019a", "l"}, // l with bar
			{"\u019e", "n"}, // n with long right leg
			{"\u01a5", "p"}, // p with hook
			{"\u01ab", "t"}, // t with palatal hook
			{"\u01ad", "t"}, // t with hook
			{"\u01b4", "y"}, // y with hook
			{"\u01b6", "z"}, // z with stroke
			{"\u01e5", "g"}, // g with stroke
			{"\u0225", "z"}, // z with hook
			{"\u0234", "l"}, // l with curl
			{"\u0235", "n"}, // n with curl
			{"\u0236", "t"}, // t with curl
			{"\u0237", "j"}, // dotless j
			{"\u023c", "c"}, // c with stroke
			{"\u023f", "s"}, // s with swash tail
			{"\u0240", "z"}, // z with swash tail
			{"\u0247", "e"}, // e with stroke
			{"\u0249", "j"}, // j with stroke
			{"\u024b", "q"}, // q with hook tail
			{"\u024d", "r"}, // r with stroke
			{"\u024f", "y"}  // y with stroke
	};


	String[][] _non_id_translate_digraphs = { { "\u00df", "sz" }, // ligature sz
		    {"\u00e6", "ae"},      // ae
		    {"\u0153", "oe"},     // ligature oe
		    {"\u0238", "db"},      // db digraph
		    {"\u0239", "qp"}      // qp digraph
		};



}
