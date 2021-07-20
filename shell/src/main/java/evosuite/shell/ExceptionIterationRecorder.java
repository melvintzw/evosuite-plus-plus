package evosuite.shell;

import static evosuite.shell.EvosuiteForMethod.projectId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.result.BranchInfo;
import org.evosuite.result.ExceptionResult;
import org.evosuite.result.ExceptionResultBranch;
import org.evosuite.result.ExceptionResultIteration;
import org.evosuite.testcase.TestChromosome;
import org.slf4j.Logger;

import evosuite.shell.excel.ExcelWriter;
import evosuite.shell.utils.LoggerUtils;

public class ExceptionIterationRecorder extends ExperimentRecorder {
	private Logger log = LoggerUtils.getLogger(ExceptionIterationRecorder.class);

	private ExcelWriter excelWriter;
	private String[] dataColumnHeaders;
	private String[] testCaseCodeColumnHeaders;
	private String[] statisticsColumnHeaders;
	private Map<String, Integer> branchIdentifierToIterationNumber = new HashMap<>();
	
	private static final String DEFAULT_SHEET = "Data";
	private static final String TEST_CASE_CODE_SHEET = "Test case code";
	private static final String STATISTICS_SHEET = "Statistics";
	
	public ExceptionIterationRecorder(int iteration) throws IOException {
		super();
	}	
	
	private String generateFileName() {
		return projectId + "_exception_data.xlsx";
	}
	
	private void setupExcelWriter() {
		excelWriter = new ExcelWriter(
			FileUtils.newFile(
					Settings.getReportFolder(),
					generateFileName()
			)
		);
	}
	
	private void setupColumnHeadersForSheet(String sheetName, String[] columnHeaders) {
		if (excelWriter == null) {
			log.error("Excel writer was not initialised.");
			return;
		}
		excelWriter.getSheet(sheetName, columnHeaders, 0);
	}
	
	private String[] generateTestCaseCodeColumnHeaders() {
		List<String> columnHeaders = new ArrayList<>();
		columnHeaders.add("Evosuite iteration");
		columnHeaders.add("Class");
		columnHeaders.add("Method");
		columnHeaders.add("Branch");
		columnHeaders.add("Is branch covered?");
		
		return columnHeaders.toArray(new String[] {});
	}
	
	private String[] generateDataColumnHeaders() {
		List<String> columnHeaders = new ArrayList<>();
		columnHeaders.add("Evosuite iteration");
		columnHeaders.add("Class");
		columnHeaders.add("Method");
		columnHeaders.add("Branch");
		columnHeaders.add("Is branch covered?");

		return columnHeaders.toArray(new String[] {});
	}
	
	private String[] generateStatisticsColumnHeaders() {
		List<String> columnHeaders = new ArrayList<>();
		columnHeaders.add("Evosuite iteration");
		columnHeaders.add("Class");
		columnHeaders.add("Method");
		columnHeaders.add("Branch");
		columnHeaders.add("Is branch covered?");
		columnHeaders.add("Total iterations");
		columnHeaders.add("Total exceptions");
		columnHeaders.add("Total in-method exceptions");
		columnHeaders.add("Breakdown of in-method exception types");
		columnHeaders.add("Total out-method exceptions");
		columnHeaders.add("Breakdown of out-method exception types");
		
		return columnHeaders.toArray(new String[] {});
	}
	
	@Override
	public void record(String className, String methodName, EvoTestResult r) {	
		if (excelWriter == null) {
			setupExcelWriter();
		}

		dataColumnHeaders = generateDataColumnHeaders();
		testCaseCodeColumnHeaders = generateTestCaseCodeColumnHeaders();
		statisticsColumnHeaders = generateStatisticsColumnHeaders();

		setupColumnHeadersForSheet(DEFAULT_SHEET, dataColumnHeaders);
		setupColumnHeadersForSheet(TEST_CASE_CODE_SHEET, testCaseCodeColumnHeaders);
		setupColumnHeadersForSheet(STATISTICS_SHEET, statisticsColumnHeaders);
		
		List<List<Object>> rows = new ArrayList<>();
		List<List<Object>> comments = new ArrayList<>();
		List<List<Object>> statistics = new ArrayList<>();
		
		ExceptionResult<TestChromosome> exceptionResult = r.getExceptionResult();
		for (ExceptionResultBranch<TestChromosome> exceptionResultBranch : exceptionResult.getAllResults()) {
			String branchName = ((BranchCoverageTestFitness) exceptionResultBranch.getFitnessFunction()).getBranch().toString();
			String branchGoal = (((BranchCoverageTestFitness) exceptionResultBranch.getFitnessFunction()).getBranchGoal().getValue() ? "true" : "false");
			String branchIdentifier = className + methodName + ";" + branchName + ";" + branchGoal;
			boolean isBranchCovered = isBranchCovered(r, exceptionResultBranch);
			
			Integer evosuiteIterationNumber = branchIdentifierToIterationNumber.get(branchIdentifier);
			if (evosuiteIterationNumber == null) {
				branchIdentifierToIterationNumber.put(branchIdentifier, 0);
				evosuiteIterationNumber = 0;
			} else {
				branchIdentifierToIterationNumber.put(branchIdentifier, evosuiteIterationNumber + 1);
			}
			
			List<Object> rowData = new ArrayList<>();
			List<Object> rowComments = new ArrayList<>();
			List<Object> rowStatistics = new ArrayList<>();
			
			rowData.add(evosuiteIterationNumber);
			rowData.add(className);
			rowData.add(methodName);
			rowData.add(branchName + ";" + branchGoal);
			rowData.add(isBranchCovered);
			
			rowComments.add(evosuiteIterationNumber);
			rowComments.add(className);
			rowComments.add(methodName);
			rowComments.add(branchName + ";" + branchGoal);
			rowComments.add(isBranchCovered);
			
			rowStatistics.add(evosuiteIterationNumber);
			rowStatistics.add(className);
			rowStatistics.add(methodName);
			rowStatistics.add(branchName + ";" + branchGoal);
			rowStatistics.add(isBranchCovered);
			
			for (ExceptionResultIteration<TestChromosome> exceptionResultIteration : exceptionResultBranch.getAllResults()) {
				boolean isExceptionOccurred = exceptionResultIteration.isExceptionOccurred();
				boolean isInMethodException = exceptionResultIteration.isInMethodException();
				String exceptionClass = "";
				if (isExceptionOccurred) {
					exceptionClass = exceptionResultIteration.getException().getClass().getCanonicalName();
				}
				
				String testCaseCode = exceptionResultIteration.getTestCase().toString();
				
				// We store all the information in a single cell per iteration.
				StringBuilder iterationEntryBuilder = new StringBuilder();
				iterationEntryBuilder.append("Exception occurred? " + (isExceptionOccurred ? "Yes" : "No"));
				iterationEntryBuilder.append("\n");
				iterationEntryBuilder.append("In/out-method exception? " + (isExceptionOccurred ? (isInMethodException ? "In-method" : "Out-method") : "-"));
				iterationEntryBuilder.append("\n");
				iterationEntryBuilder.append("Exception class: " + exceptionClass);
				iterationEntryBuilder.append("\n");
				iterationEntryBuilder.append("Stack trace: ");
				iterationEntryBuilder.append("\n");
				
				// To save the stack trace, we have to manually create a string from the stack trace.
				StringBuilder stackTraceStringBuilder = new StringBuilder();
				if (isExceptionOccurred) {
					for (StackTraceElement stackTraceElement : exceptionResultIteration.getException().getStackTrace()) {
						stackTraceStringBuilder.append(stackTraceElement.toString()).append("\n");
					}
				}
				
				iterationEntryBuilder.append(stackTraceStringBuilder.toString());
				rowData.add(iterationEntryBuilder.toString());
				
				rowComments.add(testCaseCode);
			}
			
			// Add statistics about breakdown of exceptions, etc.
			int numberOfIterations = exceptionResultBranch.getNumberOfIterations();
			int numberOfExceptions = exceptionResultBranch.getNumberOfExceptions();
			int numberOfInMethodExceptions = exceptionResultBranch.getNumberOfInMethodExceptions();
			int numberOfOutMethodExceptions = exceptionResultBranch.getNumberOfOutMethodExceptions();
			List<Throwable> exceptions = exceptionResultBranch.getExceptions();
			List<Throwable> inMethodExceptions = exceptionResultBranch.getInMethodExceptions();
			List<Throwable> outMethodExceptions = exceptionResultBranch.getOutMethodExceptions();
			Map<String, Integer> inMethodExceptionsClassified = getExceptionTypesAndCount(inMethodExceptions);
			Map<String, Integer> outMethodExceptionsClassified = getExceptionTypesAndCount(outMethodExceptions);
			
			// Verification checks
			boolean isExceptionSumEqualTotal = (numberOfExceptions == (numberOfInMethodExceptions + numberOfOutMethodExceptions));
			boolean isListSizeEqualExceptionCount = (exceptions.size() == numberOfExceptions);
			boolean isListSizeEqualInMethodExceptionCount = (inMethodExceptions.size() == numberOfInMethodExceptions);
			boolean isListSizeEqualOutMethodExceptionCount = (outMethodExceptions.size() == numberOfOutMethodExceptions);
			int sumOfInMethodExceptionCounts = 0;
			boolean isInMethodExceptionSumEqualTotal = false;
			for (Map.Entry<String, Integer> entry : inMethodExceptionsClassified.entrySet()) {
				boolean isValueNull = (entry.getValue() == null);
				if (isValueNull) {
					log.debug("Encountered a null value in the in-method exception classification map.");
					break;
				}
				
				sumOfInMethodExceptionCounts += entry.getValue();
			}
			isInMethodExceptionSumEqualTotal = (sumOfInMethodExceptionCounts == numberOfInMethodExceptions);
			
			int sumOfOutMethodExceptionCounts = 0;
			boolean isOutMethodExceptionSumEqualTotal = false;
			for (Map.Entry<String, Integer> entry : outMethodExceptionsClassified.entrySet()) {
				boolean isValueNull = (entry.getValue() == null);
				if (isValueNull) {
					log.debug("Encountered a null value in the out-method exception classification map.");
					break;
				}
				
				sumOfOutMethodExceptionCounts += entry.getValue();
			}
			isOutMethodExceptionSumEqualTotal = (sumOfOutMethodExceptionCounts == numberOfOutMethodExceptions);
			
			if (!isListSizeEqualExceptionCount) {
				log.debug("Exception count assertion was violated.");
			}
			
			if (!isListSizeEqualInMethodExceptionCount) {
				log.debug("In-method exception count assertion was violated.");
			}
			
			if (!isListSizeEqualOutMethodExceptionCount) {
				log.debug("Out-method exception count assertion was violated.");
			}
			
			if (!isExceptionSumEqualTotal) {
				log.debug("Exception total assertion was violated.");
			}
			
			if (!isInMethodExceptionSumEqualTotal) {
				log.debug("In-method exception total assertion was violated.");
			}
			
			if (!isOutMethodExceptionSumEqualTotal) {
				log.debug("Out-method exception total assertion was violated.");
			}
			
			
			StringBuilder inMethodExceptionClassificationStringBuilder = new StringBuilder();
			for (Map.Entry<String, Integer> entry : inMethodExceptionsClassified.entrySet()) {
				String exceptionClass = entry.getKey();
				Integer count = entry.getValue();
				
				inMethodExceptionClassificationStringBuilder.append(exceptionClass).append(": ").append(count).append("\n");
			}
			
			StringBuilder outMethodExceptionClassificationStringBuilder = new StringBuilder();
			for (Map.Entry<String, Integer> entry : outMethodExceptionsClassified.entrySet()) {
				String exceptionClass = entry.getKey();
				Integer count = entry.getValue();
				
				outMethodExceptionClassificationStringBuilder.append(exceptionClass).append(": ").append(count).append("\n");
			}
			
			rowStatistics.add(numberOfIterations);
			rowStatistics.add(numberOfExceptions);
			rowStatistics.add(numberOfInMethodExceptions);
			rowStatistics.add(inMethodExceptionClassificationStringBuilder.toString());
			rowStatistics.add(numberOfOutMethodExceptions);
			rowStatistics.add(outMethodExceptionClassificationStringBuilder.toString());
			
			rows.add(rowData);
			comments.add(rowComments);
			statistics.add(rowStatistics);
		}
		
		try {
			excelWriter.writeSheet(DEFAULT_SHEET, rows);
			excelWriter.writeSheet(TEST_CASE_CODE_SHEET, comments);
		    excelWriter.writeSheet(STATISTICS_SHEET, statistics);
		} catch (IOException ioe) {
			log.error("IO Error\n", ioe);
		}
	}

	@Override
	public void recordError(String className, String methodName, Exception e) {
		Integer evosuiteIterationNumber = branchIdentifierToIterationNumber.get(className + methodName);
		if (evosuiteIterationNumber == null) {
			evosuiteIterationNumber = 0;
		}
		// Don't know the branch name, information not provided here.
		String unknownBranchName = "?"; 
		
		List<Object> rowData = new ArrayList<Object>();
		rowData.add(evosuiteIterationNumber);
		rowData.add(className);
		rowData.add(methodName);
		rowData.add(unknownBranchName);

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < e.getStackTrace().length; i++) {
			StackTraceElement ste = e.getStackTrace()[i];
			String s = "class Name: " + ste.getClassName() + ", line number:  " + ste.getLineNumber() + "\n";
			sb.append(s);
		}

		rowData.add("Error:\n" + sb.toString());
		try {
			excelWriter.writeSheet(DEFAULT_SHEET, Arrays.asList(rowData));
		} catch (IOException ex) {
			log.error("IO Error\n", ex);
		}
	}

	public String getFinalReportFilePath() {
		return excelWriter.getFile().getAbsolutePath();
	}
	
	private static Map<String, Integer> getExceptionTypesAndCount(List<Throwable> exceptions) {
		Map<String, Integer> toReturn = new HashMap<>();
		
		for (Throwable exception : exceptions) {
			String exceptionType = exception.getClass().getCanonicalName();
			Integer count = toReturn.get(exceptionType);
			if (count == null) {
				toReturn.put(exceptionType, 1);
			} else {
				toReturn.put(exceptionType, count + 1);
			}		
		}
		
		return toReturn;
	}
	
	private static boolean isBranchCovered(EvoTestResult result, ExceptionResultBranch<TestChromosome> exceptionResultBranch) {
		Set<BranchInfo> coveredBranches = result.getCoveredBranchWithTest().keySet();
		Branch branch = ((BranchCoverageTestFitness) exceptionResultBranch.getFitnessFunction()).getBranch();
		BytecodeInstruction bytecodeInstruction = branch.getInstruction();
		
		for (BranchInfo branchInfo : coveredBranches) {
			boolean isClassNameSame = (branchInfo.getClassName().equals(branch.getClassName()));
			boolean isMethodNameSame = (branchInfo.getMethodName().equals(branch.getMethodName()));
			boolean isLineNumberSame = (branchInfo.getLineNo() == bytecodeInstruction.getLineNumber());
			
			if (isClassNameSame && isMethodNameSame && isLineNumberSame) {
				return true;
			}
		}
		
		return false;
	}
}
