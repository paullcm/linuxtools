/*******************************************************************************
 * Copyright (c) 2015, 2018 Open Analytics NV and others.
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.linuxtools.internal.docker.editor.syntax;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.linuxtools.internal.docker.editor.scanner.DockerCommentScanner;
import org.eclipse.linuxtools.internal.docker.editor.scanner.InstructionWordRule;

public class SyntaxProblemReporter {

	private static final String LINE_SEP = System.getProperty("line.separator");

	public void checkAndApply(IDocument document, int offset, int length, IResource resource)
			throws CoreException, BadLocationException {

		// We can't do problem report when file is external (no IResource)
		if (resource == null) {
			return;
		}
		boolean fullScan = (offset == 0 && length == document.getLength());

		// Clear any existing markers in the affected region.
		IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
		List<IMarker> markersToDelete = new ArrayList<>();
		for (IMarker marker : markers) {
			int markerLineNr = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
			int regionLineNr = document.getLineOfOffset(offset);
			if (fullScan || markerLineNr == regionLineNr)
				markersToDelete.add(marker);
		}
		for (IMarker marker : markersToDelete)
			marker.delete();

		// Apply new problems as needed.
		List<SyntaxProblem> problems = check(document, offset, length);
		for (SyntaxProblem problem : problems) {
			IMarker marker = resource.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, problem.severity);
			marker.setAttribute(IMarker.MESSAGE, problem.message);
			marker.setAttribute(IMarker.LINE_NUMBER, document.getLineOfOffset(problem.offset));
			marker.setAttribute(IMarker.CHAR_START, problem.offset);
			marker.setAttribute(IMarker.CHAR_END, problem.offset + problem.length);
		}
	}

	public List<SyntaxProblem> check(IDocument document, int offset, int length) throws BadLocationException {
		String region = document.get(offset, length);
		int lineCount = region.split(LINE_SEP).length;
		int startingLineNr = document.getLineOfOffset(offset);

		List<SyntaxProblem> problems = new ArrayList<>();

		for (int lineNr = startingLineNr; lineNr < startingLineNr + lineCount; lineNr++) {
			int lineOffset = document.getLineOffset(lineNr);
			int lineEnd = document.getLength();
			if (lineNr + startingLineNr + 1 < lineCount)
				lineEnd = document.getLineOffset(lineNr + 1);

			int endOfFirstWord = 0;
			for (; endOfFirstWord < lineEnd - lineOffset; endOfFirstWord++) {
				if (Character.isWhitespace(document.getChar(lineOffset + endOfFirstWord)))
					break;
			}

			String firstWord = document.get(lineOffset, endOfFirstWord);
			if (firstWord.trim().isEmpty())
				continue;
			if (firstWord.trim().startsWith(DockerCommentScanner.COMMENT_SEQUENCE))
				continue;

			String matchingInstruction = null;
			for (String instr : InstructionWordRule.INSTRUCTIONS) {
				if (instr.equalsIgnoreCase(firstWord)) {
					matchingInstruction = instr;
					break;
				}
			}

			if (matchingInstruction == null) {
				// Error: unknown instruction
				problems.add(new SyntaxProblem(IMarker.SEVERITY_ERROR, lineOffset, firstWord.length(),
						"Unknown instruction: " + firstWord));
			} else if (!matchingInstruction.equals(firstWord)) {
				// Warning: case incorrect
				problems.add(new SyntaxProblem(IMarker.SEVERITY_WARNING, lineOffset, firstWord.length(),
						"Instructions should be upper case"));
			}
		}

		return problems;
	}

	public static class SyntaxProblem {
		public int severity;
		public int offset;
		public int length;
		public String message;

		public SyntaxProblem(int severity, int offset, int length, String message) {
			this.severity = severity;
			this.offset = offset;
			this.length = length;
			this.message = message;
		}
	}
}
