/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package egovframework.example.sample.web;

import java.util.List;

import egovframework.example.sample.service.EgovSampleService;
import egovframework.example.sample.service.SampleDefaultVO;
import egovframework.example.sample.service.SampleVO;

import egovframework.rte.fdl.property.EgovPropertyService;
import egovframework.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springmodules.validation.commons.DefaultBeanValidator;


//@Controller로 컨트롤러 역할 선언
@Controller
public class EgovSampleController {

	//egovSampleService 빈 주입
	/** EgovSampleService */
	@Resource(name = "sampleService")
	private EgovSampleService sampleService;

	//egovPropertyService 빈 주입
	//context-properties.xml에 propertiesService라는 이름으로 등록한 빈
	/** EgovPropertyService */
	@Resource(name = "propertiesService")
	protected EgovPropertyService propertiesService;

	
	//contxt-validator.xml에 beanValidator라는 이름으로 등록한 빈 주입
	//beanValidator
	/** Validator */
	@Resource(name = "beanValidator")
	protected DefaultBeanValidator beanValidator;

	/**
	 * 글 목록을 조회한다. (pageing)
	 * @param searchVO - 조회할 정보가 담긴 SampleDefaultVO
	 * @param model
	 * @return "egovSampleList"
	 * @exception Exception
	 */
	
	// 프론트에서 /egovSampleList.do 요청에 맵핑되어 응답될 메소드
	@RequestMapping(value = "/egovSampleList.do")
	//@ModelAttribute 클라이언트의 요청 값을 SampleDefaultVO 객체형태로 전달
	//ModelMap을 통해 데이터 전달
	//searchVO는 검색여부, 조건, 단어, 페이지 정보
	public String selectSampleList(@ModelAttribute("searchVO") SampleDefaultVO searchVO, ModelMap model) throws Exception {

		/** EgovPropertyService.sample */
		//propertiesService에 지정된 값을 key로 가져오고 
		//searchVO의 setter에 기본값 지정 10, 10
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		/** pageing setting */
		
		//pom.xml에 등록된 egov.mvc -> dispatcher-servlet.xml에 등록된 페이지네이션
		PaginationInfo paginationInfo = new PaginationInfo();
		
		//searchVO의 페이징 기본 정보를 가져와 페이지네이션에 세팅
		//searchVO.getPageIndex() = 1
		paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
		//searchVO.getPageUnit() = 10
		paginationInfo.setRecordCountPerPage(searchVO.getPageUnit());
		//searchVO.getPageSize() = 10
		paginationInfo.setPageSize(searchVO.getPageSize());

		//searchVO에 페이지네이션을 통해 생성된 정보를 세팅
		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		//sampleService.selectSampleList의 파라미터로 전달
		//sampleService에 작성한 메소드 호출
		//sampleService를 구현한 sampleServiceImpl에서 sampleDao의 selectSampleList 호출
		//pom.xml의 rte.psl.dataaccess 의존성 선언으로 sql맵핑
		List<?> sampleList = sampleService.selectSampleList(searchVO);
		//모델에 resultList 이름으로 지정한 sampleList 담음
		model.addAttribute("resultList", sampleList);

		int totCnt = sampleService.selectSampleListTotCnt(searchVO);
		paginationInfo.setTotalRecordCount(totCnt);
		model.addAttribute("paginationInfo", paginationInfo);

		//지정된 view name을 스프링 dispatcherservlet에 리턴
		//dispatcher servlet이 view resolver에 view name에 전달하여 필요한 view를 호출함
		return "sample/egovSampleList";
	}

	/**
	 * 글 등록 화면을 조회한다.
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param model
	 * @return "egovSampleRegister"
	 * @exception Exception
	 */
	//addSample.do을 get 메소드로 요청할 경우 맵핑 
	@RequestMapping(value = "/addSample.do", method = RequestMethod.GET)
	public String addSampleView(@ModelAttribute("searchVO") SampleDefaultVO searchVO, Model model) throws Exception {
		//searchVO 객체 생성하여 등록
		model.addAttribute("sampleVO", new SampleVO());
		return "sample/egovSampleRegister";
	}

	/**
	 * 글을 등록한다.
	 * @param sampleVO - 등록할 정보가 담긴 VO
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param status
	 * @return "forward:/egovSampleList.do"
	 * @exception Exception
	 */
	//addSample.do을 post 메소드로 요청할 경우 맵핑 
	@RequestMapping(value = "/addSample.do", method = RequestMethod.POST)
	public String addSample(@ModelAttribute("searchVO") SampleDefaultVO searchVO, SampleVO sampleVO, BindingResult bindingResult, Model model, SessionStatus status)
			throws Exception {

		//서버 사이드 검증
		//글 등록 시 유효성 검사
		//pom.xml에 commons-validator 의존성 추가
		//context-validator에 beanValidator 등록
		//빈 프로퍼티 validatorFactory에 검증할 규칙을 적용
		beanValidator.validate(sampleVO, bindingResult);

		//유효성 검사 후 에러시 등록화면 다시 출력
		if (bindingResult.hasErrors()) {
			model.addAttribute("sampleVO", sampleVO);
			return "sample/egovSampleRegister";
		}

		//sampleVO를 파라미터로 sampleService의 insertSample 호출, db에 입력
		sampleService.insertSample(sampleVO);
		//세션을 저장된 model을 삭제
		status.setComplete();
		
		//글 등록 시 egovSampleList.do로 포워딩
		return "forward:/egovSampleList.do";
	}

	/**
	 * 글 수정화면을 조회한다.
	 * @param id - 수정할 글 id
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param model
	 * @return "egovSampleRegister"
	 * @exception Exception
	 */
	///updateSampleView.do 요청할 경우 맵핑 
	@RequestMapping("/updateSampleView.do")
	//url의 파라미터로 전달된 selectedId를 @RequestParam로 메소드의 파라미터로 사용
	public String updateSampleView(@RequestParam("selectedId") String id, @ModelAttribute("searchVO") SampleDefaultVO searchVO, Model model) throws Exception {
		//SampleVO 객체 생성 후 파라미터 id 값 세팅
		SampleVO sampleVO = new SampleVO();
		sampleVO.setId(id);
		//업데이트 화면을 위해 하나의 게시글 선택하고 전달
		model.addAttribute(selectSample(sampleVO, searchVO));
		return "sample/egovSampleRegister";
	}

	/**
	 * 글을 조회한다.
	 * @param sampleVO - 조회할 정보가 담긴 VO
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param status
	 * @return @ModelAttribute("sampleVO") - 조회한 정보
	 * @exception Exception
	 */
	//하나의 게시글을 선택하는 메소드
	public SampleVO selectSample(SampleVO sampleVO, @ModelAttribute("searchVO") SampleDefaultVO searchVO) throws Exception {
		return sampleService.selectSample(sampleVO);
	}

	/**
	 * 글을 수정한다.
	 * @param sampleVO - 수정할 정보가 담긴 VO
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param status
	 * @return "forward:/egovSampleList.do"
	 * @exception Exception
	 */
	///updateSample.do 요청할 경우 맵핑 
	@RequestMapping("/updateSample.do")
	public String updateSample(@ModelAttribute("searchVO") SampleDefaultVO searchVO, SampleVO sampleVO, BindingResult bindingResult, Model model, SessionStatus status)
			throws Exception {

		//서버 사이드 검증
		//글 등록 시 유효성 검사
		//pom.xml에 commons-validator 의존성 추가
		//context-validator에 beanValidator 등록
		//빈 프로퍼티 validatorFactory에 검증할 규칙을 적용
		beanValidator.validate(sampleVO, bindingResult);

		//유효성 검사 후 에러시 등록화면 다시 출력
		if (bindingResult.hasErrors()) {
			model.addAttribute("sampleVO", sampleVO);
			return "sample/egovSampleRegister";
		}
		
		//sampleVO를 파라미터로 sampleService의 updateSample 호출, db정보 수정
		sampleService.updateSample(sampleVO);
		//세션을 저장된 model을 삭제
		status.setComplete();
		//리스트 화면으로 포워딩
		return "forward:/egovSampleList.do";
	}

	/**
	 * 글을 삭제한다.
	 * @param sampleVO - 삭제할 정보가 담긴 VO
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param status
	 * @return "forward:/egovSampleList.do"
	 * @exception Exception
	 */
	///deleteSample.do 요청할 경우 맵핑  
	@RequestMapping("/deleteSample.do")
	public String deleteSample(SampleVO sampleVO, @ModelAttribute("searchVO") SampleDefaultVO searchVO, SessionStatus status) throws Exception {
		//전달된 sampleVO 파라미터로  sampleService의 deleteSample 메소드 호출
		sampleService.deleteSample(sampleVO);
		//세션 종료
		status.setComplete();
		//삭제 후 게시글 리스트화면으로 포워딩
		return "forward:/egovSampleList.do";
	}

}
